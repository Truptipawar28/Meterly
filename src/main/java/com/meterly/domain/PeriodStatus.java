package com.meterly.domain;

/** A billing period is OPEN while usage is recorded, then CLOSED when invoiced. */
public enum PeriodStatus {
    OPEN,
    CLOSED
}
