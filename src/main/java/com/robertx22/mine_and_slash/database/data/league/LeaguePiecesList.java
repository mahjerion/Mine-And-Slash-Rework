package com.robertx22.mine_and_slash.database.data.league;

import java.util.ArrayList;
import java.util.List;

public class LeaguePiecesList {

    public List<LeagueStructurePieces> list = new ArrayList<>();

    public LeaguePiecesList(List<LeagueStructurePieces> list) {
        this.list = list;
    }

    // .. why does this exist?
    public LeagueStructurePieces get(String id) {
        return list.stream().filter(x -> x.folder.equals(id)).findAny().get();
    }

    public LeagueStructurePieces get() {
        return list.stream().findAny().get();
    }

}
