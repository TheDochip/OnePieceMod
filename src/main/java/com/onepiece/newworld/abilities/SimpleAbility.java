package com.onepiece.newworld.abilities;

/** Способность, логика которой задана лямбдой. Подходит для большинства одноразовых атак. */
public class SimpleAbility extends Ability {
    @FunctionalInterface
    public interface Action {
        boolean run(AbilityContext context);
    }

    private final Action action;

    public SimpleAbility(int baseCooldownTicks, float energyCost, Action action) {
        super(baseCooldownTicks, energyCost);
        this.action = action;
    }

    @Override
    public boolean activate(AbilityContext context) {
        return action.run(context);
    }
}
