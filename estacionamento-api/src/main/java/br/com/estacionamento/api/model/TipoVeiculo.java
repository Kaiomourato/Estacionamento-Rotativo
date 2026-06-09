package br.com.estacionamento.api.model;

public enum TipoVeiculo {
    MOTO(1),        // ocupa 1 slot
    CARRO(2),       // ocupa 2 slots (vaga de carro = 2 slots = 2 motos)
    CAMINHONETE(3); // ocupa 3 slots (vaga de caminhonete = 3 slots = 3 motos)

    private final int slots;

    TipoVeiculo(int slots) {
        this.slots = slots;
    }

    public int getSlots() {
        return slots;
    }
}
