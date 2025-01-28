package com.robertx22.mine_and_slash.maps.dungeon_reg;

import com.robertx22.mine_and_slash.maps.room_adders.*;
import com.robertx22.mine_and_slash.mmorpg.SlashRef;
import com.robertx22.mine_and_slash.tags.all.DungeonTags;

public class Dungeons {

    public static void init() {
        Dungeon.Builder.of("test", "Test Dungeon", new TestRoomAdder(), SlashRef.MODID).weight(0).build();

        Dungeon.Builder.of("cement", "Cemetery", new CementeryAdder(), SlashRef.MODID).weight(2000).build();

        Dungeon.Builder.of("warped", "Warped Forest", new WarpedRoomAdder(), SlashRef.MODID).tags(DungeonTags.FOREST).weight(2000).build();
        Dungeon.Builder.of("wn", "Nature's End", new WideNatureRoomAdder(), SlashRef.MODID).tags(DungeonTags.FOREST).weight(2000).build();
        Dungeon.Builder.of("bastion", "The Bastion", new BastionRoomAdder(), SlashRef.MODID).weight(2000).build();
        Dungeon.Builder.of("sewer2", "Slime Sewers", new Sewer2RoomAdder(), SlashRef.MODID).weight(2000).build();

        // todo fix this dungeon, nvm removed because uses summon commands instead of normal mob spawning
        //Dungeon.Builder.of("pyramid", "The Pyramid", new PyramidRoomAdder()).weight(100).build();


        Dungeon.Builder.of("nature", "Natural", new NatureRoomAdder(), SlashRef.MODID).tags(DungeonTags.FOREST).weight(300).build();
        Dungeon.Builder.of("brick", "Brickhouse", new BrickRoomAdder(), SlashRef.MODID).setIsOnlyAsAdditionalRooms().weight(100).build();
        Dungeon.Builder.of("it", "Ice Temple", new IceTempleRoomAdder(), SlashRef.MODID).weight(1000).build();
        Dungeon.Builder.of("mossy_brick", "Mossy Temple", new MossyBrickRoomAdder(), SlashRef.MODID).weight(750).build();
        Dungeon.Builder.of("nether", "The Nether", new NetherRoomAdder(), SlashRef.MODID).weight(750).build();
        Dungeon.Builder.of("mine", "The Mineshaft", new MineRoomAdder(), SlashRef.MODID).weight(600).build();
        Dungeon.Builder.of("sandstone", "Sandstone", new SandstoneRoomAdder(), SlashRef.MODID).weight(800).build();

        Dungeon.Builder.of("sewers", "Sewers", new SewersRoomAdder(), SlashRef.MODID).weight(200).build();
        Dungeon.Builder.of("spruce_mansion", "Spruce Mansion", new SpruceMansionRoomAdder(), SlashRef.MODID).weight(700).build();
        Dungeon.Builder.of("stone_brick", "Stone Brick", new StoneBrickRoomAdder(), SlashRef.MODID).weight(1000).build();
        Dungeon.Builder.of("tent", "Giant Tents", new TentRoomAdder(), SlashRef.MODID).weight(600).build();

        Dungeon.Builder.of("steampunk", "Steampunk", new SteampunkRoomAdder(), SlashRef.MODID).weight(50).build();

        Dungeon.Builder.of("misc", "Random", new MiscGroupAdders(), SlashRef.MODID).setIsOnlyAsAdditionalRooms().weight(0).build();

    }
}
