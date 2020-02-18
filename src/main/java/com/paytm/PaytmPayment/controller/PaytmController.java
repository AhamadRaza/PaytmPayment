package com.paytm.PaytmPayment.controller;

import com.paytm.PaytmPayment.model.PaytmDetails;
import com.paytm.pg.merchant.CheckSumServiceHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.TreeMap;

@Controller
public class PaytmController {
    @Autowired PaytmDetails paytmDetails;
    @Autowired private Environment env;

    @GetMapping("/")
    public String home(){
        return "home";
    }

    @PostMapping("/pageredirect")
    public ModelAndView pageRedirect(@RequestParam(name = "CUST_ID") String customerId,
                                     @RequestParam(name = "TXN_AMOUNT") String transactionAmount,
                                     @RequestParam(name = "ORDER_ID") String orderId) throws Exception {
        ModelAndView modelAndView = new ModelAndView("redirect:" + paytmDetails.getPaytmUrl());
        TreeMap<String, String> parameters = new TreeMap<>();
        paytmDetails.getDetails().forEach((k,v) -> parameters.put(k, v));
        parameters.put("MOBILE_NO", env.getProperty("paytm.mobile"));
        parameters.put("EMAIL", env.getProperty("paytm.email"));
        parameters.put("ORDER_ID", orderId);
        parameters.put("TXN_AMOUNT", transactionAmount);
        parameters.put("CUST_ID", customerId);
        String checkSum  = getCheckSum(parameters);
        parameters.put("CHECKSUMHASH", checkSum);
        modelAndView.addAllObjects(parameters);
        return modelAndView;
    }

    @PostMapping(value = "/pageresponse")
    public String getReasponseRedirect(HttpServletRequest request, Model model){
        Map<String, String[]> mapData = request.getParameterMap();
        TreeMap<String, String> parameters = new TreeMap<>();
        mapData.forEach((key, val) -> parameters.put(key, val[0]));
        String paytmCheckSum = "";
        if(mapData.containsKey("CHECKSUMHASH")){
            paytmCheckSum = mapData.get("CHECKSUMHASH")[0];
        }
        String result;
        boolean isValidateCheckSum = false;
        System.out.println("RESULT = " + parameters.toString());

        try {
            isValidateCheckSum = validateCheckSum(parameters, paytmCheckSum);

            if (isValidateCheckSum && parameters.containsKey("RESPCODE")) {
                if (parameters.get("RESPCODE").equals("01")) {
                    result = "payment successfull";
                } else
                    result = "payment failed";
            } else {
                result = "checksum mismatched";
            }
        }
        catch (Exception e) {
            result = e.toString();
        }
        model.addAttribute("result", result);
        parameters.remove("CHECKSUMHASH");
        model.addAttribute("parameters", parameters);
        return "report";
    }

    private boolean validateCheckSum(TreeMap<String, String> parameters, String paytmCheckSum) throws Exception {
        return CheckSumServiceHelper.getCheckSumServiceHelper().verifycheckSum(paytmDetails.getMerchantKey(), parameters, paytmCheckSum);
    }

    private String getCheckSum(TreeMap<String, String> parameters) throws Exception {
        return CheckSumServiceHelper.getCheckSumServiceHelper().genrateCheckSum(paytmDetails.getMerchantKey(), parameters);
    }
}