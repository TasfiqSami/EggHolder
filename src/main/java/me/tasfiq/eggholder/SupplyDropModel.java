package me.tasfiq.eggholder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Bundled, locally rendered BDEngine Airdrop model (model 8073). */
final class SupplyDropModel {
    private final List<Part> parts;

    private SupplyDropModel(List<Part> parts) {
        this.parts = List.copyOf(parts);
    }

    static SupplyDropModel load(JavaPlugin plugin) throws IOException {
        try (InputStream input = plugin.getResource("supply-drop-model.yml")) {
            if (input == null) {
                throw new IOException("Bundled supply-drop-model.yml is missing");
            }
            var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
                    new InputStreamReader(input, StandardCharsets.UTF_8));
            List<Part> parts = new ArrayList<>();
            for (var section : yaml.getMapList("parts")) {
                Object materialValue = section.get("material");
                Material material = materialValue == null ? null : Material.matchMaterial(materialValue.toString());
                Object matrixValue = section.get("matrix");
                if (material == null || !material.isBlock() || !(matrixValue instanceof List<?> values) || values.size() != 16) {
                    throw new IOException("Invalid supply drop model part in bundled model data");
                }
                float[] matrix = new float[16];
                for (int index = 0; index < matrix.length; index++) {
                    if (!(values.get(index) instanceof Number number)) {
                        throw new IOException("Invalid transform value in bundled supply drop model");
                    }
                    matrix[index] = number.floatValue();
                }
                parts.add(new Part(material, matrix));
            }
            if (parts.isEmpty()) {
                throw new IOException("Bundled supply drop model contains no display parts");
            }
            return new SupplyDropModel(parts);
        }
    }

    List<UUID> spawn(World world, Location origin) {
        List<UUID> spawned = new ArrayList<>(parts.size());
        try {
            for (Part part : parts) {
                BlockDisplay display = world.spawn(part.location(origin), BlockDisplay.class, entity -> {
                    entity.setPersistent(false);
                    entity.setInvulnerable(true);
                    entity.setGravity(false);
                    entity.setBlock(org.bukkit.Bukkit.createBlockData(part.material()));
                    entity.setBillboard(Display.Billboard.FIXED);
                    entity.setInterpolationDuration(0);
                    entity.setTransformation(part.transformation());
                });
                spawned.add(display.getUniqueId());
            }
            return List.copyOf(spawned);
        } catch (RuntimeException exception) {
            remove(spawned);
            throw exception;
        }
    }

    boolean move(List<UUID> entities, Location origin) {
        if (entities.size() != parts.size()) {
            return false;
        }
        for (int index = 0; index < entities.size(); index++) {
            Entity entity = org.bukkit.Bukkit.getEntity(entities.get(index));
            if (!(entity instanceof BlockDisplay display) || !display.isValid()) {
                return false;
            }
            display.teleport(parts.get(index).location(origin));
        }
        return true;
    }

    void remove(List<UUID> entities) {
        for (UUID id : entities) {
            Entity entity = org.bukkit.Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private record Part(Material material, float[] matrix) {
        private Location location(Location origin) {
            return origin.clone().add(matrix[3], matrix[7], matrix[11]);
        }

        private Transformation transformation() {
            Vector3f scale = new Vector3f(
                    length(matrix[0], matrix[4], matrix[8]),
                    length(matrix[1], matrix[5], matrix[9]),
                    length(matrix[2], matrix[6], matrix[10]));
            float determinant = matrix[0] * (matrix[5] * matrix[10] - matrix[6] * matrix[9])
                    - matrix[1] * (matrix[4] * matrix[10] - matrix[6] * matrix[8])
                    + matrix[2] * (matrix[4] * matrix[9] - matrix[5] * matrix[8]);
            if (determinant < 0.0F) {
                scale.x = -scale.x;
            }
            float sx = nonZero(scale.x);
            float sy = nonZero(scale.y);
            float sz = nonZero(scale.z);
            Matrix4f rotationMatrix = new Matrix4f()
                    .m00(matrix[0] / sx).m10(matrix[4] / sx).m20(matrix[8] / sx)
                    .m01(matrix[1] / sy).m11(matrix[5] / sy).m21(matrix[9] / sy)
                    .m02(matrix[2] / sz).m12(matrix[6] / sz).m22(matrix[10] / sz);
            Quaternionf rotation = rotationMatrix.getUnnormalizedRotation(new Quaternionf());
            return new Transformation(new Vector3f(), rotation, scale, new Quaternionf());
        }

        private static float length(float x, float y, float z) {
            return (float) Math.sqrt(x * x + y * y + z * z);
        }

        private static float nonZero(float value) {
            return Math.abs(value) < 1.0e-6F ? 1.0F : value;
        }
    }
}
