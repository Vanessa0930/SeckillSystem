package test.java.com.seckillservice.integration;

import main.java.com.seckillservice.service.SeckillService;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

public class SubmitRequestIntegTest extends AbstractJavaSamplerClient {
    private String itemId;

    @Override
    public void setupTest(JavaSamplerContext javaSamplerContext) {
        itemId = javaSamplerContext.getParameter("ItemId");
    }

    @Override
    public SampleResult runTest(JavaSamplerContext args) {
        SampleResult result = new SampleResult();
        try {
            result.sampleStart();
            SeckillService service = new SeckillService();
            String res = service.submitRequest(itemId);
            result.setSuccessful(true);
            result.setResponseCodeOK();
            result.setResponseData(res, null);
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
        System.out.println("Tear down test");
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument("ItemId", "1");
        return params;
    }
}
