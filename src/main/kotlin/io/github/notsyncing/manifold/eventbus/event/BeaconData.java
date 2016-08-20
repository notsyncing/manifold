package io.github.notsyncing.manifold.eventbus.event;

import io.github.notsyncing.manifold.eventbus.ManifoldEventNode;

public class BeaconData
{
    private String id;
    private String[] groups;
    private int load;

    public BeaconData()
    {
    }

    public BeaconData(ManifoldEventNode node)
    {
        this.id = node.getId();
        this.groups = node.getGroups();
        this.load = node.getLoad();
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String[] getGroups()
    {
        return groups;
    }

    public void setGroups(String[] groups)
    {
        this.groups = groups;
    }

    public int getLoad()
    {
        return load;
    }

    public void setLoad(int load)
    {
        this.load = load;
    }
}
