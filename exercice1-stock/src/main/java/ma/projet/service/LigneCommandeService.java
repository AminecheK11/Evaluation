package ma.projet.service;

import ma.projet.classes.Commande;
import ma.projet.classes.Produit;
import ma.projet.classes.LigneCommandeProduit;
import ma.projet.classes.LigneCommandeProduitId;

public class LigneCommandeService extends AbstractService<LigneCommandeProduit> {
    public LigneCommandeService() { super(LigneCommandeProduit.class); }

    @Override
    public boolean create(LigneCommandeProduit ligne) {
        return inTransaction(session -> {
            // @MapsId exige des parents attaches a la session de persistence.
            Commande commande = session.get(Commande.class, ligne.getId().getCommandeId());
            Produit produit = session.get(Produit.class, ligne.getId().getProduitId());
            if (commande == null || produit == null) {
                throw new IllegalArgumentException("Enregistrer la commande et le produit avant leur ligne.");
            }
            session.persist(new LigneCommandeProduit(commande, produit, ligne.getQuantite()));
            return true;
        });
    }

    @Override
    public LigneCommandeProduit findById(java.io.Serializable id) {
        return inTransaction(session -> session.createQuery("""
                select l from LigneCommandeProduit l
                join fetch l.produit p join fetch p.categorie join fetch l.commande
                where l.id = :id
                """, LigneCommandeProduit.class).setParameter("id", id).uniqueResult());
    }
}
