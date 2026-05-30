package edu.bondarchukdo.otpproject.channel;

import java.nio.charset.StandardCharsets;

import edu.bondarchukdo.otpproject.config.SmppProperties;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsOtpSender {

    private static final Logger log = LoggerFactory.getLogger(SmsOtpSender.class);

    private final SmppProperties smppProperties;

    public SmsOtpSender(SmppProperties smppProperties) {
        this.smppProperties = smppProperties;
    }

    public void sendCode(String destination, String code) {
        SMPPSession session = new SMPPSession();
        try {
            BindParameter bindParameter = new BindParameter(
                    BindType.BIND_TX,
                    smppProperties.systemId(),
                    smppProperties.password(),
                    smppProperties.systemType(),
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    smppProperties.sourceAddr());

            session.connectAndBind(smppProperties.host(), smppProperties.port(), bindParameter);

            session.submitShortMessage(
                    smppProperties.systemType(),
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    smppProperties.sourceAddr(),
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    destination,
                    new ESMClass(),
                    (byte) 0,
                    (byte) 1,
                    null,
                    null,
                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT),
                    (byte) 0,
                    new GeneralDataCoding(Alphabet.ALPHA_DEFAULT),
                    (byte) 0,
                    ("Your code: " + code).getBytes(StandardCharsets.UTF_8));

            log.info("SMPP SMS submitted to {} via {}:{}", destination, smppProperties.host(), smppProperties.port());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to send SMS via SMPP: " + e.getMessage(), e);
        } finally {
            session.unbindAndClose();
        }
    }
}
