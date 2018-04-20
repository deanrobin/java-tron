package org.tron.core.witness.freeze;

import org.tron.core.capsule.AccountCapsule;
import org.tron.core.capsule.FreezeAccountCapsule;

//No freeze in funds
public class FreezeStrategyNone implements FreezeStrategy {

  @Override
  public boolean isFreezeAllowed(FreezeAccountCapsule fbo, FreezePolicyContext context) {
    return true;
  }

  @Override
  public void freeze(FreezeAccountCapsule fbo, AccountCapsule accountCapsule,
      FreezePolicyContext context, AccountModifiedResult accountModifiedResult) {
    //Direct deposit of account balance
    accountCapsule.setBalance(accountCapsule.getBalance() + context.amount);
    accountModifiedResult.isAccountModified = true;
  }

  @Override
  public boolean isWithdrawAllowed(FreezeAccountCapsule fbo, withdrawPolicyContext context) {
    return context.amount == 0;
  }

  @Override
  public long getAllowedWithdraw(FreezeAccountCapsule fbo, withdrawPolicyContext context) {
    return 0;
  }

  @Override
  public void withdraw(FreezeAccountCapsule fbo, AccountCapsule accountCapsule,
      withdrawPolicyContext context, AccountModifiedResult accountModifiedResult) {
  }
}