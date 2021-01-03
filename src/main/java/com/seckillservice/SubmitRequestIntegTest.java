package main.java.com.seckillservice;

import main.java.com.seckillservice.service.SeckillService;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

import static main.java.com.seckillservice.utils.constants.FAILED_RESULT;

public class SubmitRequestIntegTest extends AbstractJavaSamplerClient {
    private String itemId;
    private SeckillService service;

    @Override
    public void setupTest(JavaSamplerContext javaSamplerContext) {
        itemId = javaSamplerContext.getParameter("ItemId");
        service = SeckillService.getInstance();
    }

    @Override
    public SampleResult runTest(JavaSamplerContext args) {
        SampleResult result = new SampleResult();
        try {
            result.sampleStart();
            String res = service.submitRequest(itemId);
            result.setSuccessful(!res.equals(FAILED_RESULT));
            result.setResponseData(res, null);
        } catch (Exception e) {
            result.setSuccessful(false);
            result.setResponseMessage(e.getMessage());
            e.printStackTrace();
        } finally {
            result.sampleEnd();
        }
        return result;
    }

    @Override
    public void teardownTest(JavaSamplerContext javaSamplerContext) {
        System.out.println("Tear down SubmitRequestIntegTest");
    }

    @Override
    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument("ItemId", "1");
        return params;
    }
}
