package guru.springframework.msscssm.services;

import guru.springframework.msscssm.domain.Payment;
import guru.springframework.msscssm.domain.PaymentEvent;
import guru.springframework.msscssm.domain.PaymentState;
import guru.springframework.msscssm.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService {

    public static final String PAYMENT_ID_HEADER = "payment_id";

    private final PaymentRepository paymentRepository;

    private final StateMachineFactory<PaymentState, PaymentEvent> stateMachineFactory;

    private final PaymentStateChangeListener paymentStateChangeListener;

    @Override
    public Payment newPayment(Payment payment) {

        payment.setState(PaymentState.NEW);
        return paymentRepository.save(payment);
    }

    @Override
    public StateMachine<PaymentState, PaymentEvent> preAuth(Long paymentId) {

        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.PRE_AUTHORIZE);
        return null;
    }

    @Override
    public StateMachine<PaymentState, PaymentEvent> authorize(Long paymentId) {

        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.AUTHORIZE_APPROVED);

        return null;
    }

    @Override
    public StateMachine<PaymentState, PaymentEvent> declineAuth(Long paymentId) {

        StateMachine<PaymentState, PaymentEvent> sm = build(paymentId);

        sendEvent(paymentId, sm, PaymentEvent.AUTHORIZE_DECLINED);
        return null;
    }

    private void sendEvent(Long paymentId, StateMachine<PaymentState, PaymentEvent> sm, PaymentEvent paymentEvent) {

        Message<PaymentEvent> msg = MessageBuilder.withPayload(paymentEvent).setHeader(PAYMENT_ID_HEADER, paymentId).build();

        sm.sendEvent(msg);
    }

    //Will get the state from the db and return a state machine in that state
    private StateMachine<PaymentState, PaymentEvent> build(Long paymentId) {

        Payment payment = paymentRepository.getOne(paymentId);

        StateMachine<PaymentState, PaymentEvent> sm = stateMachineFactory.getStateMachine(Long.toString(paymentId));

        sm.stop();

        sm.getStateMachineAccessor().doWithAllRegions(sma -> {
            sma.resetStateMachine(new DefaultStateMachineContext<>(payment.getState(), null, null, null));
            sma.addStateMachineInterceptor(paymentStateChangeListener);
        });

        sm.start();

        return sm;
    }
}
