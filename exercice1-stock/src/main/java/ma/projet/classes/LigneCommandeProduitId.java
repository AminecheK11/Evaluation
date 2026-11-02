package ma.projet.classes;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class LigneCommandeProduitId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "commande_id")
    private int commandeId;
    @Column(name = "produit_id")
    private int produitId;

    public LigneCommandeProduitId() { }
    public LigneCommandeProduitId(int commandeId, int produitId) {
        this.commandeId = commandeId;
        this.produitId = produitId;
    }
    public int getCommandeId() { return commandeId; }
    public int getProduitId() { return produitId; }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof LigneCommandeProduitId other)) return false;
        return commandeId == other.commandeId && produitId == other.produitId;
    }
    @Override
    public int hashCode() { return Objects.hash(commandeId, produitId); }
}
