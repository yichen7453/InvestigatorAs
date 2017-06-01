package sensor.fpc.hst.fpcsensor;

public class Fingerprint {

    // CAC related variables can't modify!!!!
    public int shiftValue = 5;
    public int gainValue = 5;
    public int px1Value = 5;
    public int cacRowStart = 0;
    public int cacRowLength = 0;
    public int cacColStart = 0;
    public int cacColLength = 0;
    public int kFramePixelsPerAdcGroup = 8;

    static {
        System.loadLibrary("share_cac");
    }

    public native boolean calibrateInit(int modelName);
    public native boolean calibrateImage(byte[] image);

    public native boolean cacInit();
    public native int cacImage(byte[] image);

    public native int preprocessorInit();
    public native void preprocessorCleanup();
    public native int preprocessor(byte[] image);
}
