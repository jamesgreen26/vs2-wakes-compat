package g_mungus.wakes_compat;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.duck.ProducesWake;
import com.goby56.wakes.simulation.WakeHandler;
import com.goby56.wakes.simulation.WakeNode;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3i;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.api.world.LevelYRange;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import java.util.*;

import static g_mungus.wakes_compat.Util.*;
import static g_mungus.wakes_compat.VSWakesCompat.CONFIG;
import static g_mungus.wakes_compat.VSWakesCompat.getSeaLevel;

public class ShipWake {

    public static void placeWakeTrail(Ship ship) {
        WakeHandler wakeHandler = WakeHandler.getInstance();
        if (wakeHandler != null) {
            ProducesWake producer = (ProducesWake)ship;
            double velocity = producer.getHorizontalVelocity();
            float height = producer.producingHeight();
            Iterator var7;
            WakeNode node;

            Vec3d prevPos = producer.getPrevPos();

//            AABBdc aabb = ship.getWorldAABB();
//            AABBic saabb = ship.getShipAABB();
            if (prevPos != null) {
//                assert saabb != null;
//                float xwidth = saabb.maxX() - saabb.minX();
//                float zwidth = saabb.maxZ() - saabb.minZ();
//                float width = Math.min(xwidth, zwidth);

                float width = ((DynamicWakeSize)ship).vs_wakes_compat_template_1_20_1$getWidth();

                if (width > CONFIG.maxWidth()) return;

                double toX = ((DynamicWakeSize)ship).vs_wakes_compat_template_1_20_1$getPos().x;
                double toY = ((DynamicWakeSize)ship).vs_wakes_compat_template_1_20_1$getPos().z;

//                assert MinecraftClient.getInstance().player != null;
//                MinecraftClient.getInstance().player.sendMessage(Text.of("WAKE_COORDS: " + toX + ", " + toY));

                var7 = WakeNode.Factory.thickNodeTrail(prevPos.x, prevPos.z, toX, toY, height, (float) WakesClient.CONFIG_INSTANCE.initialStrength, velocity, width).iterator();

                while(var7.hasNext()) {
                    node = (WakeNode)var7.next();
                    wakeHandler.insert(node);
                }

            }
        }
    }

    public static void checkShipSize(Ship s) {

        World world = MinecraftClient.getInstance().world;

        Vector3dc horizontalVelocity = new Vector3d(s.getVelocity().x(), 0, s.getVelocity().z());

        if (horizontalVelocity.length() < 0.05) return;


        double velAngle = horizontalVelocity.angleSigned(new Vector3d(0, 0, -1), new Vector3d(0, 1, 0));


        double shipAngle = getYaw(s.getTransform().getShipToWorldRotation());



        Direction direction = approximateDirection(Math.toDegrees(velAngle - shipAngle));

        Vec3d shipPos = Util.getCentre(s.getWorldAABB());

        Double yLevelShip = VectorConversionsMCKt.toJOML(new Vec3d(shipPos.x, getSeaLevel(), shipPos.z)).mulPosition(s.getWorldToShip()).y;

        int blockYLevelShip = yLevelShip.intValue();


        Vector3i minWorldPos = new Vector3i();
        Vector3i maxWorldPos = new Vector3i();

        // Rounding minY down to the nearest multiple of 16
        int minY = (blockYLevelShip / 16) * 16;

        // Rounding maxY up to the nearest multiple of 16, then subtracting 1 to get congruent to 15
        int maxY = ((blockYLevelShip / 16) * 16) + 15;


        s.getActiveChunksSet().getMinMaxWorldPos(minWorldPos, maxWorldPos, new LevelYRange(minY, maxY));



        if (VSWakesCompat.CONFIG.shouldSkipWakesFromSide()) {

            AABBic shipBounds = s.getShipAABB();
            assert shipBounds != null;
            int shipX = shipBounds.maxX() - shipBounds.minX();
            int shipZ = shipBounds.maxZ() - shipBounds.minZ();

            if ((direction == Direction.NORTH || direction == Direction.SOUTH) && (shipX > shipZ)) {
                ((DynamicWakeSize) s).vs_wakes_compat_template_1_20_1$setWidth(0);
                return;
            } else if ((direction == Direction.EAST || direction == Direction.WEST) && (shipZ > shipX)) {
                ((DynamicWakeSize) s).vs_wakes_compat_template_1_20_1$setWidth(0);
                return;
            }
        }

        calculateShipWidthAndOffset(world, minWorldPos, maxWorldPos, blockYLevelShip, direction, s);
    }


    private static void calculateShipWidthAndOffset(World world, Vector3i minWorldPos, Vector3i maxWorldPos,
                                                    int blockYLevelShip, Direction direction, Ship s) {
        // Axis variables to abstract x-axis or z-axis iterations
        boolean isZAxis = (direction == Direction.NORTH || direction == Direction.SOUTH);


        int primaryMin = isZAxis ? minWorldPos.z() : minWorldPos.x();  // Min for the primary iteration axis
        int primaryMax = isZAxis ? maxWorldPos.z() : maxWorldPos.x();  // Max for the primary iteration axis
        int secondaryMin = isZAxis ? minWorldPos.x() : minWorldPos.z();  // Min for the secondary axis
        int secondaryMax = isZAxis ? maxWorldPos.x() : maxWorldPos.z();  // Max for the secondary axis
        boolean isNegativeDirection = (direction == Direction.NORTH || direction == Direction.WEST); // Reverse iteration?

        List<LinkedList<BlockPos>> rows = new ArrayList<>();

        for (int primary = isNegativeDirection ? primaryMax : primaryMin;
             isNegativeDirection ? primary >= primaryMin : primary <= primaryMax;
             primary += isNegativeDirection ? -1 : 1) {

            LinkedList<BlockPos> blockPositions = new LinkedList<>();


            for (int secondary = secondaryMin; secondary <= secondaryMax; secondary++) {
                int x = isZAxis ? secondary : primary;  // Set x for BlockPos
                int z = isZAxis ? primary : secondary;  // Set z for BlockPos

                if (!world.getBlockState(new BlockPos(x, blockYLevelShip, z)).isAir()) {

                    blockPositions.add(new BlockPos(x, blockYLevelShip, z));
                }
            }
            if (!blockPositions.isEmpty()) {
                rows.add(blockPositions);
            }
            if (rows.size() >= 5) break;
        }

        // Use the widest row to calculate the final width and offset
        float width = 0;
        Vector3d offset = new Vector3d();


        for(LinkedList<BlockPos> row : rows) {
            float rowWidth = getWidth(row);
            if (rowWidth > width && rowWidth <= CONFIG.maxWidth()) {
                width = rowWidth;
                offset = getOffset(row);
                offset = new Vector3d(offset.x, 0, offset.z);
            }
        }




        ((DynamicWakeSize) s).vs_wakes_compat_template_1_20_1$setWidth(width);

        Vector3d offsetReal;

        if (!offset.equals(0,0,0)) {
            Vector3d shipCentre = Util.getCentre(Objects.requireNonNull(s.getShipAABB()));
            offsetReal = new Vector3d(shipCentre.x, 0, shipCentre.z).sub(offset);
            ((DynamicWakeSize) s).vs_wakes_compat_template_1_20_1$setOffset(offsetReal.negate());
        }





//        assert MinecraftClient.getInstance().player != null;
//        MinecraftClient.getInstance().player.sendMessage(Text.of("Offset: " + offsetReal));
    }

    private static int getWidth(LinkedList<BlockPos> blockPositions) {
        if(blockPositions.isEmpty()) return 0;

        return blockPositions.getFirst().getManhattanDistance(blockPositions.getLast()) + 1;
    }

    private static Vector3d getOffset(LinkedList<BlockPos> blockPositions) {
        if (blockPositions.isEmpty()) return new Vector3d();

        Vec3d first = Vec3d.ofCenter(blockPositions.getFirst());
        Vec3d last = Vec3d.ofCenter(blockPositions.getLast());

        return averageVec(first, last);
    }



}
