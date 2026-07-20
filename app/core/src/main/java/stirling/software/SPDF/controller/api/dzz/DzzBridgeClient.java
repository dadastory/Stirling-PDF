package stirling.software.SPDF.controller.api.dzz;

public interface DzzBridgeClient {

    DzzBridgeResponse input(String job);

    DzzBridgeResponse save(String job, String name, byte[] content);
}
