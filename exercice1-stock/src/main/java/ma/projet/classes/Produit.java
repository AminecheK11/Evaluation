package ma.projet.classes;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "produit")
@NamedQuery(name = "Produit.prixSuperieur100",
        query = "select p from Produit p join fetch p.categorie where p.prix > 100 order by p.reference")
public class Produit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private float prix;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @OneToMany(mappedBy = "produit")
    private List<LigneCommandeProduit> lignesCommande = new ArrayList<>();

    public Produit() { }

    public Produit(String reference, float prix, Categorie categorie) {
        this.reference = reference;
        setPrix(prix);
        setCategorie(categorie);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public float getPrix() { return prix; }
    public void setPrix(float prix) {
        if (!Float.isFinite(prix) || prix < 0) {
            throw new IllegalArgumentException("Le prix doit etre positif ou nul.");
        }
        this.prix = prix;
    }
    public Categorie getCategorie() { return categorie; }
    public void setCategorie(Categorie categorie) {
        if (categorie == null) throw new IllegalArgumentException("Categorie obligatoire.");
        this.categorie = categorie;
    }
    public List<LigneCommandeProduit> getLignesCommande() { return lignesCommande; }

    @Override
    public String toString() { return reference + " | " + prix + " DH"; }
}
