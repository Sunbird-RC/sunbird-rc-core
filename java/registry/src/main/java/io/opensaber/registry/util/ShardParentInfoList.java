package io.opensaber.registry.util;

import java.util.ArrayList;
import java.util.List;

public class ShardParentInfoList {
    private List<ShardParentInfo> parentInfos = new ArrayList<>();

    public List<ShardParentInfo> getParentInfos() {
        return parentInfos;
    }

    public void setParentInfos(List<ShardParentInfo> parentInfos) {
        this.parentInfos = parentInfos;
    }
}
