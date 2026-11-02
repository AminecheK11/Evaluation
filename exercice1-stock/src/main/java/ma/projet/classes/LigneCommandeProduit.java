package ma.projet.classes;

import javax.persistence.*;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "ligne_commande_produit")
@Check(constraints = "quantite > 0")
public class LigneCommandeProduit {
    @EmbeddedId
    private LigneCommandeProduitId id = new LigneCommandeProduitId();

    @MapsId("commandeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "commande_id", nullable = false)
    private Commande commande;

    @MapsId("produitId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produit_id", nullable = false)
    private Produit produit;

    @Column(nullable = false)
    private int quantite;

    public LigneCommandeProduit() { }

    public LigneCommandeProduit(Commande commande, Produit produit, int quantite) {
        if (commande == null || produit == null) {
            throw new IllegalArgumentException("Commande et produit obligatoires.");
        }
        this.commande = commande;
        this.produit = produit;
        this.id = new LigneCommandeProduitId(commande.getId(), produit.getId());
        setQuantite(quantite);
    }

    public LigneCommandeProduitId getId() { return id; }
    public Commande getCommande() { return commande; }
    public Produit getProduit() { return produit; }
    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) {
        if (quantite <= 0) throw new IllegalArgumentException("La quantite doit etre strictement positive.");
        this.quantite = quantite;
    }
}
