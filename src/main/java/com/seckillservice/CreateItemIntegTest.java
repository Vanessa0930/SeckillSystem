package main.java.com.seckillservice;

import main.java.com.seckillservice.common.models.Inventory;
import main.java.com.seckillservice.handler.InventoryHandler;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

public class CreateItemIntegTest extends AbstractJavaSamplerClient {
    private String itemName;
    private int itemCount;
    private InventoryHandler inventoryHandler;
    private final String ITEM_NAME = "ItemName";
    private final String COUNT = "Count";

    @Override
    public void setupTest(JavaSamplerContext args) {
        itemName = args.getParameter(ITEM_NAME);
        itemCount = Integer.valueOf(args.getParameter(COUNT));
        inventoryHandler = new InventoryHandler();
    }

    @Override
    public SampleResult runTest(JavaSamplerContext args) {
        SampleResult result = new SampleResult();
        try {
            result.sampleStart();
            Inventory item = inventoryHandler.createInventory(itemName, itemCount);
            result.setSuccessful(true);
            result.setResponseCodeOK();
            result.setResponseData("Successfully create inventory item: " + item.prettyPrintRecord(), null);
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setResponseCode("ERROR");
            result.setResponseMessage(e.getMessage());
            e.printStackTrace();
        } finally {
            result.sampleEnd();
        }
        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext javaSamplerContext) {
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments args = new Arguments();
        args.addArgument(ITEM_NAME, "MyItem");
        args.addArgument(COUNT, "20");
        return args;
    }
}
