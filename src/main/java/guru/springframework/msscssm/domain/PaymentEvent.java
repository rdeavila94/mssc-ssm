package guru.springframework.msscssm.domain;

public enum PaymentEvent {
    PRE_AUTHORIZE,
    PRE_AUTH_APPROVED,
    PRE_AUTH_DECLINED,
    AUTHORIZE,
    AUTHORIZE_APPROVED,
    AUTHORIZE_DECLINED
}
