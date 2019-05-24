package iot.example.devices.hardware;

public final class DriverException extends Exception {
    private static final long serialVersionUID = 6164921645357791803L;

    public final int rc;
    public final String rd;
    public final String bpd;

    public DriverException(final int rc, final String rd) {
        this(rc, rd, rd);
    }


    public DriverException(final int rc, final String rd, final String bpd) {
        super(String.format("[%d] %s%s", rc, rd, ((bpd == null) ? "" : (" (" + bpd + ")"))));
        this.rc = rc;
        this.rd = rd;
        this.bpd = bpd;
    }

    public final int getResultCode() {
        return this.rc;
    }
}
