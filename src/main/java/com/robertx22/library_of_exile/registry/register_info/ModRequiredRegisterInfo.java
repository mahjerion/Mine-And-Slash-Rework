package com.robertx22.library_of_exile.registry.register_info;

public class ModRequiredRegisterInfo {

    public String modid;

    public HardcodedRegistration hard;
    public SeriazableRegistration ser;

    public ModRequiredRegisterInfo(String modid) {
        this.modid = modid;

        this.hard = new HardcodedRegistration(modid);
        this.ser = new SeriazableRegistration(modid);
    }
}
