package ma.projet.service;

import ma.projet.classes.*;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProduitService extends AbstractService<Produit> {
    public ProduitService() { super(Produit.class); }

    public List<Produit> findByCategorie(int categorieId) {
        return inTransaction(session -> session.createQuery("""
                select p from Produit p join fetch p.categorie
                where p.categorie.id = :categorieId order by p.reference
                """, Produit.class).setParameter("categorieId", categorieId).getResultList());
    }

    /** Bornes inclusives ; les dates sont des jours calendaires comme Commande.date. */
    public List<Produit> findCommandesEntreDates(Date debut, Date fin) {
        if (debut == null || fin == null || debut.after(fin)) {
            throw new IllegalArgumentException("Intervalle de dates invalide.");
        }
        return inTransaction(session -> session.createQuery("""
                select distinct p from LigneCommandeProduit l
                join l.produit p join fetch p.categorie join l.commande c
                where c.date between :debut and :fin order by p.reference
                """, Produit.class)
                .setParameter("debut", debut, javax.persistence.TemporalType.DATE)
                .setParameter("fin", fin, javax.persistence.TemporalType.DATE)
                .getResultList());
    }

    /** Retourne les lignes pour conserver la quantite de chaque produit. */
    public List<LigneCommandeProduit> findByCommande(int commandeId) {
        return inTransaction(session -> session.createQuery("""
                select l from LigneCommandeProduit l
                join fetch l.produit p join fetch p.categorie join fetch l.commande
                where l.commande.id = :commandeId order by p.reference
                """, LigneCommandeProduit.class).setParameter("commandeId", commandeId).getResultList());
    }

    public List<Produit> findPrixSuperieur100() {
        return inTransaction(session -> session
                .createNamedQuery("Produit.prixSuperieur100", Produit.class).getResultList());
    }

    public void afficherProduitsCommande(int commandeId, PrintStream out) {
        Commande commande = new CommandeService().findById(commandeId);
        if (commande == null) throw new IllegalArgumentException("Commande inexistante : " + commandeId);
        SimpleDateFormat format = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);
        out.println("Commande : " + commande.getId());
        out.println("Date : " + format.format(commande.getDate()));
        out.println("\nListe des produits :");
        out.printf("%-16s %-14s %s%n", "Référence", "Prix", "Quantité");
        DecimalFormat prix = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.FRANCE));
        for (LigneCommandeProduit ligne : findByCommande(commandeId)) {
            out.printf(Locale.FRANCE, "%-16s %-14s %d%n", ligne.getProduit().getReference(),
                    prix.format(ligne.getProduit().getPrix()) + " DH", ligne.getQuantite());
        }
    }
}
