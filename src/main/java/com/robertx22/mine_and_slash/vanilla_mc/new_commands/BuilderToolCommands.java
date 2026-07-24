package com.robertx22.mine_and_slash.vanilla_mc.new_commands;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.dungeon_realm.database.DungeonDatabase;
import com.robertx22.dungeon_realm.database.dungeon.Dungeon;
import com.robertx22.library_of_exile.command_wrapper.*;
import com.robertx22.library_of_exile.dimension.structure.dungeon.DungeonRoom;
import com.robertx22.library_of_exile.dimension.structure.dungeon.RoomType;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.StructureBlockEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BuilderToolCommands {
    public static void reg(CommandDispatcher dis) {
        CommandBuilder.of(CommandRefs.ID, dis, b -> {
            PlayerWrapper enarg = new PlayerWrapper();
            var RADIUS = new IntWrapper("radius");
            var HEIGHT = new IntWrapper("height");
            var ROOM_SIZE = new IntWrapper("room_size");

            b.addLiteral("builder_tool_warning", PermWrapper.OP);
            b.addLiteral("generate_structure_blocks_per_chunk", PermWrapper.OP);
            b.addArg(RADIUS);
            b.addArg(HEIGHT);
            b.addArg(enarg);
            // blocks per side of each structure block; must be a multiple of 16. 16 = one per chunk
            // (old behaviour), 32 spaces them 2 chunks apart at 32x32, etc.
            b.addArg(ROOM_SIZE);

            b.action(e -> {

                Player p = enarg.get(e);

                if (!p.isCreative()) {
                    p.sendSystemMessage(Component.literal("You must be in creative mode to use this command. This is extra safety to make sure this command isn't usable accidentally."));
                    return;
                }
                ServerLevel world = (ServerLevel) p.level();

                BlockPos pos = p.blockPosition();
                int radius = RADIUS.get(e);
                int height = HEIGHT.get(e);
                int roomSize = ROOM_SIZE.get(e);
                ChunkPos cp = new ChunkPos(pos);

                if (roomSize < 16 || roomSize % 16 != 0) {
                    p.sendSystemMessage(Component.literal("room_size must be a multiple of 16 (16, 32, 48, 64...)."));
                    return;
                }
                int step = roomSize / 16;

                // radius is in chunks; step by the room's chunk width so blocks tile edge-to-edge
                for (int x = 0; x < radius; x += step) {
                    for (int z = 0; z < radius; z += step) {
                        var cpos = new ChunkPos(cp.x + x, cp.z + z);
                        var fpos = cpos.getBlockAt(0, pos.getY(), 0);
                        //world.setBlock(fpos, Blocks.STRUCTURE_BLOCK.defaultBlockState(), 2);
                        StructureUtils.createNewEmptyStructureBlock(x + "_" + z, fpos, new BlockPos(roomSize, height, roomSize), Rotation.NONE, world);
                    }
                }
            });

        }, "Gens structure blocks in radius, sized room_size (blocks) and spaced to tile edge-to-edge");

        CommandBuilder.of(CommandRefs.ID, dis, x -> {
            PlayerWrapper enarg = new PlayerWrapper();
            var DUNGEON = new RegistryWrapper<Dungeon>(DungeonDatabase.DUNGEON);

            x.addLiteral("builder_tool_warning", PermWrapper.OP);
            x.addLiteral("generate_dungeon_pieces", PermWrapper.OP);
            x.addArg(DUNGEON);

            x.addArg(enarg);

            x.action(e -> {

                Player p = enarg.get(e);

                if (!p.isCreative()) {
                    p.sendSystemMessage(Component.literal("You must be in creative mode to use this command. This is extra safety to make sure this command isn't usable accidentally."));
                    return;
                }
                var world = p.level();

                BlockPos pos = p.blockPosition();
                Dungeon dungeon = DUNGEON.getFromRegistry(e);
                ChunkPos cp = new ChunkPos(pos);

                // space pieces by the dungeon's own footprint (+ a gap) so bigger rooms don't overlap
                int spacing = dungeon.getDungeonData().room_size + 4;

                int i = 0;
                int z = 0;
                for (RoomType type : RoomType.values()) {
                    z = 0;
                    for (String room : dungeon.data.getRoomList(type)) {
                        var aroom = new DungeonRoom(dungeon.getDungeonData().folder, room, type);
                        var roomPos = new BlockPos(cp.getMinBlockX() + (i * spacing), pos.getY(), cp.getMaxBlockZ() + (z * spacing));

                        world.setBlock(roomPos, Blocks.STRUCTURE_BLOCK.defaultBlockState(), 2);

                        if (world.getBlockEntity(roomPos) instanceof StructureBlockEntity be) {
                            be.setStructureName(aroom.loc);
                            be.setStructurePos(new BlockPos(0, 0, 0).above().north());
                            be.loadStructure((ServerLevel) world);
                        }

                        z++;
                    }
                    i++;
                }

                p.sendSystemMessage(Component.literal("Use the load_nearby_structures command if you want to load them too."));


            });

        }, "Gens all dungeon pieces sorting them by type");

        CommandBuilder.of(CommandRefs.ID, dis, x -> {
            PlayerWrapper enarg = new PlayerWrapper();

            x.addLiteral("builder_tool_warning", PermWrapper.OP);
            x.addLiteral("load_nearby_structures", PermWrapper.OP);

            x.addArg(enarg);

            x.action(e -> {

                Player p = enarg.get(e);

                if (!p.isCreative()) {
                    p.sendSystemMessage(Component.literal("You must be in creative mode to use this command. This is extra safety to make sure this command isn't usable accidentally."));
                    return;
                }

                var world = p.level();


                List<ChunkPos> terrainChunks = new ArrayList<>();
                terrainChunks.add(new ChunkPos(p.blockPosition()));
                ChunkPos start = new ChunkPos(p.blockPosition());

                int terrain = 5;

                for (int i = -terrain; i < terrain; i++) {
                    for (int z = -terrain; z < terrain; z++) {
                        terrainChunks.add(new ChunkPos(start.x + i, start.z + z));
                    }
                }

                for (ChunkPos cp : terrainChunks) {
                    var bes = new HashMap<>(world.getChunk(cp.x, cp.z).getBlockEntities());

                    for (Map.Entry<BlockPos, BlockEntity> en : bes.entrySet()) {
                        if (en.getValue() instanceof StructureBlockEntity be) {
                            be.loadStructure((ServerLevel) world);
                        }
                    }
                }

            });

        }, "Loads all structure blocks");
    }
}
