package me.tasfiq.eggholder;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

final class SpongeSchematic {

    private final int width;
    private final int height;
    private final int length;
    private final List<PlacedBlock> nonAirBlocks;
    private final List<String> unsupportedStates;
    private final int[] highestYByColumn;
    private final String[] topStateByColumn;
    private final int centerX;
    private final int centerY;
    private final int centerZ;

    private SpongeSchematic(
            int width,
            int height,
            int length,
            List<PlacedBlock> nonAirBlocks,
            List<String> unsupportedStates,
            int[] highestYByColumn,
            String[] topStateByColumn,
            int centerX,
            int centerY,
            int centerZ
    ) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.nonAirBlocks = nonAirBlocks;
        this.unsupportedStates = unsupportedStates;
        this.highestYByColumn = highestYByColumn;
        this.topStateByColumn = topStateByColumn;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
    }

    static SpongeSchematic load(InputStream inputStream) throws IOException {
        try (InputStream input = new BufferedInputStream(inputStream)) {
            NbtReader reader = new NbtReader(input);
            NbtTag root = reader.readNamedRoot();
            Map<String, NbtTag> rootCompound = expectCompound(root.value());
            Object schematicValue = rootCompound.containsKey("Schematic")
                    ? rootCompound.get("Schematic").value()
                    : rootCompound;
            Map<String, NbtTag> schematic = expectCompound(schematicValue);

            int width = numberValue(schematic.get("Width"));
            int height = numberValue(schematic.get("Height"));
            int length = numberValue(schematic.get("Length"));
            if (width <= 0 || height <= 0 || length <= 0) {
                throw new IOException("Invalid schematic dimensions: " + width + "x" + height + "x" + length);
            }

            Map<String, NbtTag> blocks = expectCompound(tagValue(schematic, "Blocks"));
            Map<String, NbtTag> paletteCompound = expectCompound(tagValue(blocks, "Palette"));
            byte[] encodedData = expectByteArray(tagValue(blocks, "Data"));

            int highestPaletteId = -1;
            Map<Integer, String> paletteStates = new LinkedHashMap<>();
            for (Map.Entry<String, NbtTag> entry : paletteCompound.entrySet()) {
                int paletteId = numberValue(entry.getValue());
                highestPaletteId = Math.max(highestPaletteId, paletteId);
                paletteStates.put(paletteId, entry.getKey());
            }
            if (highestPaletteId < 0) {
                throw new IOException("The schematic palette was empty.");
            }

            String[] blockStates = new String[highestPaletteId + 1];
            BlockData[] blockDataCache = new BlockData[highestPaletteId + 1];
            List<String> unsupportedStates = new ArrayList<>();
            for (Map.Entry<Integer, String> entry : paletteStates.entrySet()) {
                int paletteId = entry.getKey();
                String state = entry.getValue();
                blockStates[paletteId] = state;
                if (!isAirState(state)) {
                    blockDataCache[paletteId] = createCompatibleBlockData(state);
                    if (blockDataCache[paletteId] == null) {
                        unsupportedStates.add(state);
                    }
                }
            }

            int expectedBlockCount = width * height * length;
            List<PlacedBlock> nonAirBlocks = new ArrayList<>(Math.max(1024, expectedBlockCount / 8));
            int[] highestYByColumn = new int[width * length];
            Arrays.fill(highestYByColumn, -1);
            String[] topStateByColumn = new String[width * length];

            int dataIndex = 0;
            for (int blockIndex = 0; blockIndex < expectedBlockCount; blockIndex++) {
                int paletteId = readVarInt(encodedData, dataIndex);
                dataIndex = nextVarIntIndex(encodedData, dataIndex);
                if (paletteId < 0 || paletteId >= blockStates.length) {
                    throw new IOException("Palette index out of range: " + paletteId);
                }

                int x = blockIndex % width;
                int columnIndex = blockIndex / width;
                int z = columnIndex % length;
                int y = columnIndex / length;

                String state = blockStates[paletteId];
                if (state == null || isAirState(state)) {
                    continue;
                }

                int topIndex = x + z * width;
                highestYByColumn[topIndex] = y;
                topStateByColumn[topIndex] = state;

                BlockData blockData = blockDataCache[paletteId];
                if (blockData != null) {
                    nonAirBlocks.add(new PlacedBlock(x, y, z, blockData));
                }
            }

            int centerX = width / 2;
            int centerZ = length / 2;
            int centerColumnIndex = centerX + centerZ * width;
            int centerY = highestYByColumn[centerColumnIndex] < 0 ? 1 : highestYByColumn[centerColumnIndex] + 1;
            return new SpongeSchematic(width, height, length, nonAirBlocks, List.copyOf(unsupportedStates), highestYByColumn, topStateByColumn, centerX, centerY, centerZ);
        }
    }

    int width() {
        return width;
    }

    int height() {
        return height;
    }

    int length() {
        return length;
    }

    int centerX() {
        return centerX;
    }

    int centerY() {
        return centerY;
    }

    int centerZ() {
        return centerZ;
    }

    List<PlacedBlock> nonAirBlocks() {
        return nonAirBlocks;
    }

    List<String> unsupportedStates() {
        return unsupportedStates;
    }

    List<PedestalAnchor> detectCrystalPedestals(double minDistance, double maxDistance, int maxCount) {
        List<CandidateColumn> candidates = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                int columnIndex = x + z * width;
                int topY = highestYByColumn[columnIndex];
                if (topY < Math.max(15, centerY + 4)) {
                    continue;
                }

                String topState = topStateByColumn[columnIndex];
                if (!isPedestalTopState(topState)) {
                    continue;
                }

                double distance = Math.hypot(x - centerX, z - centerZ);
                if (distance < minDistance || distance > maxDistance) {
                    continue;
                }

                candidates.add(new CandidateColumn(x, z, topY));
            }
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        boolean[] visited = new boolean[candidates.size()];
        List<PedestalAnchor> anchors = new ArrayList<>();
        for (int index = 0; index < candidates.size(); index++) {
            if (visited[index]) {
                continue;
            }

            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(index);
            visited[index] = true;

            int count = 0;
            double totalX = 0.0D;
            double totalZ = 0.0D;
            int topY = Integer.MIN_VALUE;
            while (!queue.isEmpty()) {
                int currentIndex = queue.removeFirst();
                CandidateColumn candidate = candidates.get(currentIndex);
                count++;
                totalX += candidate.x();
                totalZ += candidate.z();
                topY = Math.max(topY, candidate.topY());

                for (int otherIndex = 0; otherIndex < candidates.size(); otherIndex++) {
                    if (visited[otherIndex]) {
                        continue;
                    }

                    CandidateColumn other = candidates.get(otherIndex);
                    int dx = candidate.x() - other.x();
                    int dz = candidate.z() - other.z();
                    if (dx * dx + dz * dz <= 36) {
                        visited[otherIndex] = true;
                        queue.addLast(otherIndex);
                    }
                }
            }

            if (count == 0) {
                continue;
            }

            int anchorX = (int) Math.round(totalX / count);
            int anchorZ = (int) Math.round(totalZ / count);
            double angle = Math.atan2(anchorZ - centerZ, anchorX - centerX);
            anchors.add(new PedestalAnchor(anchorX, topY + 1, anchorZ, angle));
        }

        anchors.sort(Comparator.comparingDouble(PedestalAnchor::angle));
        if (maxCount > 0 && anchors.size() > maxCount) {
            return evenlySample(anchors, maxCount);
        }
        return List.copyOf(anchors);
    }

    private static List<PedestalAnchor> evenlySample(List<PedestalAnchor> anchors, int limit) {
        if (limit <= 0 || anchors.isEmpty() || anchors.size() <= limit) {
            return List.copyOf(anchors);
        }

        List<PedestalAnchor> selected = new ArrayList<>(limit);
        for (int index = 0; index < limit; index++) {
            int sampleIndex = (int) Math.floor((double) index * anchors.size() / limit);
            sampleIndex = Math.max(0, Math.min(anchors.size() - 1, sampleIndex));
            selected.add(anchors.get(sampleIndex));
        }
        return List.copyOf(selected);
    }

    private static boolean isPedestalTopState(String state) {
        if (state == null) {
            return false;
        }
        String normalized = state.toLowerCase(Locale.ROOT);
        return normalized.startsWith("minecraft:bedrock") || normalized.startsWith("minecraft:obsidian");
    }

    private static BlockData createCompatibleBlockData(String rawState) {
        for (String candidate : createParseCandidates(rawState)) {
            try {
                return Bukkit.createBlockData(candidate);
            } catch (IllegalArgumentException ignored) {
            }
        }

        String materialName = stripProperties(stripNamespace(rawState)).toUpperCase(Locale.ROOT);
        Material material = Material.matchMaterial(materialName);
        if (material != null && material.isBlock()) {
            return material.createBlockData();
        }
        return null;
    }

    private static List<String> createParseCandidates(String rawState) {
        String strippedNamespace = stripNamespace(rawState);
        List<String> candidates = new ArrayList<>();
        addStateVariants(candidates, rawState);
        addStateVariants(candidates, strippedNamespace);
        return candidates;
    }

    private static void addStateVariants(List<String> candidates, String rawState) {
        if (rawState == null || rawState.isBlank()) {
            return;
        }

        addCandidate(candidates, rawState);
        int propertiesIndex = rawState.indexOf('[');
        if (propertiesIndex < 0 || !rawState.endsWith("]")) {
            return;
        }

        String blockId = rawState.substring(0, propertiesIndex);
        String propertiesSection = rawState.substring(propertiesIndex + 1, rawState.length() - 1);
        if (propertiesSection.isBlank()) {
            addCandidate(candidates, blockId);
            return;
        }

        List<String> properties = new ArrayList<>(List.of(propertiesSection.split(",")));
        if (properties.isEmpty()) {
            addCandidate(candidates, blockId);
            return;
        }

        addCandidatesFromPropertySubsets(candidates, blockId, properties);
        addCandidate(candidates, blockId);
    }

    private static void addCandidatesFromPropertySubsets(List<String> candidates, String blockId, List<String> properties) {
        int propertyCount = properties.size();
        if (propertyCount <= 0) {
            return;
        }

        if (propertyCount > 10) {
            for (int index = 0; index < propertyCount; index++) {
                List<String> reduced = new ArrayList<>(properties);
                reduced.remove(index);
                addCandidate(candidates, blockId + "[" + String.join(",", reduced) + "]");
            }
            return;
        }

        Set<Integer> seenMasks = new HashSet<>();
        for (int subsetSize = propertyCount - 1; subsetSize >= 1; subsetSize--) {
            List<Integer> indices = new ArrayList<>();
            for (int index = 0; index < propertyCount; index++) {
                indices.add(index);
            }
            combinePropertyIndices(candidates, blockId, properties, indices, subsetSize, 0, new ArrayList<>(), seenMasks);
        }
    }

    private static void combinePropertyIndices(
            List<String> candidates,
            String blockId,
            List<String> properties,
            List<Integer> indices,
            int targetSize,
            int startIndex,
            List<Integer> current,
            Set<Integer> seenMasks
    ) {
        if (current.size() == targetSize) {
            int mask = 0;
            List<String> subset = new ArrayList<>(targetSize);
            for (int selectedIndex : current) {
                mask |= 1 << selectedIndex;
                subset.add(properties.get(selectedIndex));
            }
            if (seenMasks.add(mask)) {
                addCandidate(candidates, blockId + "[" + String.join(",", subset) + "]");
            }
            return;
        }

        for (int index = startIndex; index <= indices.size() - (targetSize - current.size()); index++) {
            current.add(indices.get(index));
            combinePropertyIndices(candidates, blockId, properties, indices, targetSize, index + 1, current, seenMasks);
            current.remove(current.size() - 1);
        }
    }

    private static void addCandidate(List<String> candidates, String candidate) {
        if (candidate == null || candidate.isBlank() || candidates.contains(candidate)) {
            return;
        }
        candidates.add(candidate);
    }

    private static String stripNamespace(String state) {
        if (state == null) {
            return null;
        }
        int propertiesIndex = state.indexOf('[');
        String blockId = propertiesIndex < 0 ? state : state.substring(0, propertiesIndex);
        int namespaceIndex = blockId.indexOf(':');
        if (namespaceIndex >= 0) {
            String suffix = propertiesIndex < 0 ? "" : state.substring(propertiesIndex);
            return blockId.substring(namespaceIndex + 1) + suffix;
        }
        return state;
    }

    private static String stripProperties(String state) {
        if (state == null) {
            return null;
        }
        int propertiesIndex = state.indexOf('[');
        return propertiesIndex < 0 ? state : state.substring(0, propertiesIndex);
    }

    private static boolean isAirState(String state) {
        return state == null || state.equalsIgnoreCase("minecraft:air") || state.equalsIgnoreCase("air");
    }

    private static Object tagValue(Map<String, NbtTag> compound, String key) throws IOException {
        NbtTag tag = compound.get(key);
        if (tag == null) {
            throw new IOException("Missing schematic tag: " + key);
        }
        return tag.value();
    }

    private static Map<String, NbtTag> expectCompound(Object value) throws IOException {
        if (value instanceof Map<?, ?> rawMap) {
            Map<String, NbtTag> compound = new HashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof NbtTag tag) {
                    compound.put(key, tag);
                }
            }
            return compound;
        }
        throw new IOException("Expected a compound tag in the schematic data.");
    }

    private static byte[] expectByteArray(Object value) throws IOException {
        if (value instanceof byte[] byteArray) {
            return byteArray;
        }
        throw new IOException("Expected a byte-array block data payload.");
    }

    private static int numberValue(NbtTag tag) throws IOException {
        if (tag == null || !(tag.value() instanceof Number number)) {
            throw new IOException("Expected a numeric schematic tag.");
        }
        return number.intValue();
    }

    private static int readVarInt(byte[] data, int startIndex) throws IOException {
        int value = 0;
        int position = 0;
        int index = startIndex;
        while (true) {
            if (index >= data.length) {
                throw new EOFException("Unexpected end of schematic block data.");
            }
            int current = data[index] & 0xFF;
            value |= (current & 0x7F) << position;
            if ((current & 0x80) == 0) {
                return value;
            }
            position += 7;
            if (position > 28) {
                throw new IOException("Encountered an invalid VarInt in the schematic.");
            }
            index++;
        }
    }

    private static int nextVarIntIndex(byte[] data, int startIndex) throws IOException {
        int index = startIndex;
        while (true) {
            if (index >= data.length) {
                throw new EOFException("Unexpected end of schematic block data.");
            }
            int current = data[index] & 0xFF;
            index++;
            if ((current & 0x80) == 0) {
                return index;
            }
        }
    }

    record PlacedBlock(int x, int y, int z, BlockData blockData) {
    }

    record PedestalAnchor(int x, int y, int z, double angle) {
    }

    private record CandidateColumn(int x, int z, int topY) {
    }

    private record NbtTag(byte type, Object value) {
    }

    private static final class NbtReader {
        private final InputStream input;

        private NbtReader(InputStream input) throws IOException {
            this.input = new java.util.zip.GZIPInputStream(input);
        }

        private NbtTag readNamedRoot() throws IOException {
            byte type = readByte();
            if (type == 0) {
                throw new IOException("The schematic root tag was empty.");
            }
            readString();
            return new NbtTag(type, readPayload(type));
        }

        private Object readPayload(byte type) throws IOException {
            return switch (type) {
                case 1 -> readByte();
                case 2 -> readShort();
                case 3 -> readInt();
                case 4 -> readLong();
                case 5 -> Float.intBitsToFloat(readInt());
                case 6 -> Double.longBitsToDouble(readLong());
                case 7 -> readByteArray();
                case 8 -> readString();
                case 9 -> readList();
                case 10 -> readCompound();
                case 11 -> readIntArray();
                case 12 -> readLongArray();
                default -> throw new IOException("Unsupported NBT tag type: " + type);
            };
        }

        private Map<String, NbtTag> readCompound() throws IOException {
            Map<String, NbtTag> compound = new LinkedHashMap<>();
            while (true) {
                byte type = readByte();
                if (type == 0) {
                    return compound;
                }

                String name = readString();
                compound.put(name, new NbtTag(type, readPayload(type)));
            }
        }

        private List<Object> readList() throws IOException {
            byte childType = readByte();
            int length = readInt();
            List<Object> list = new ArrayList<>(Math.max(0, length));
            for (int index = 0; index < length; index++) {
                list.add(readPayload(childType));
            }
            return list;
        }

        private byte[] readByteArray() throws IOException {
            int length = readInt();
            byte[] bytes = new byte[length];
            readFully(bytes);
            return bytes;
        }

        private int[] readIntArray() throws IOException {
            int length = readInt();
            int[] values = new int[length];
            for (int index = 0; index < length; index++) {
                values[index] = readInt();
            }
            return values;
        }

        private long[] readLongArray() throws IOException {
            int length = readInt();
            long[] values = new long[length];
            for (int index = 0; index < length; index++) {
                values[index] = readLong();
            }
            return values;
        }

        private String readString() throws IOException {
            int length = readUnsignedShort();
            byte[] bytes = new byte[length];
            readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private byte readByte() throws IOException {
            int value = input.read();
            if (value < 0) {
                throw new EOFException("Unexpected end of schematic stream.");
            }
            return (byte) value;
        }

        private short readShort() throws IOException {
            return (short) readUnsignedShort();
        }

        private int readUnsignedShort() throws IOException {
            int high = input.read();
            int low = input.read();
            if ((high | low) < 0) {
                throw new EOFException("Unexpected end of schematic stream.");
            }
            return (high << 8) | low;
        }

        private int readInt() throws IOException {
            byte[] bytes = new byte[4];
            readFully(bytes);
            return ((bytes[0] & 0xFF) << 24)
                    | ((bytes[1] & 0xFF) << 16)
                    | ((bytes[2] & 0xFF) << 8)
                    | (bytes[3] & 0xFF);
        }

        private long readLong() throws IOException {
            byte[] bytes = new byte[8];
            readFully(bytes);
            return ((long) (bytes[0] & 0xFF) << 56)
                    | ((long) (bytes[1] & 0xFF) << 48)
                    | ((long) (bytes[2] & 0xFF) << 40)
                    | ((long) (bytes[3] & 0xFF) << 32)
                    | ((long) (bytes[4] & 0xFF) << 24)
                    | ((long) (bytes[5] & 0xFF) << 16)
                    | ((long) (bytes[6] & 0xFF) << 8)
                    | (bytes[7] & 0xFFL);
        }

        private void readFully(byte[] bytes) throws IOException {
            int offset = 0;
            while (offset < bytes.length) {
                int read = input.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    throw new EOFException("Unexpected end of schematic stream.");
                }
                offset += read;
            }
        }
    }
}
