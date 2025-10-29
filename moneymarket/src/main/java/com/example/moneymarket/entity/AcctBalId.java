package com.example.moneymarket.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AcctBalId implements Serializable {
    
    private LocalDate tranDate;
    private String accountNo;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AcctBalId acctBalId = (AcctBalId) o;
        return tranDate.equals(acctBalId.tranDate) && accountNo.equals(acctBalId.accountNo);
    }
    
    @Override
    public int hashCode() {
        return tranDate.hashCode() + accountNo.hashCode();
    }
}
