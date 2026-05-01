package com.carsrecommend.system.vo;

import java.math.BigDecimal;

public class DemandWeightsVO {

    private BigDecimal price;
    private BigDecimal space;
    private BigDecimal safety;
    private BigDecimal energy;
    private BigDecimal intelligence;
    private BigDecimal comfort;
    private BigDecimal power;
    private BigDecimal reputation;
    private BigDecimal popularity;

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getSpace() {
        return space;
    }

    public void setSpace(BigDecimal space) {
        this.space = space;
    }

    public BigDecimal getSafety() {
        return safety;
    }

    public void setSafety(BigDecimal safety) {
        this.safety = safety;
    }

    public BigDecimal getEnergy() {
        return energy;
    }

    public void setEnergy(BigDecimal energy) {
        this.energy = energy;
    }

    public BigDecimal getIntelligence() {
        return intelligence;
    }

    public void setIntelligence(BigDecimal intelligence) {
        this.intelligence = intelligence;
    }

    public BigDecimal getComfort() {
        return comfort;
    }

    public void setComfort(BigDecimal comfort) {
        this.comfort = comfort;
    }

    public BigDecimal getPower() {
        return power;
    }

    public void setPower(BigDecimal power) {
        this.power = power;
    }

    public BigDecimal getReputation() {
        return reputation;
    }

    public void setReputation(BigDecimal reputation) {
        this.reputation = reputation;
    }

    public BigDecimal getPopularity() {
        return popularity;
    }

    public void setPopularity(BigDecimal popularity) {
        this.popularity = popularity;
    }
}
