package ma.projet.test;

import ma.projet.classes.*;
import ma.projet.service.*;
import ma.projet.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.sql.Date;

/** Demonstration executable : mvn exec:java. Les donnees restent visibles dans MySQL. */
public class TestStock {
    public static void main(String[] args) {
        try {
            verifierConnexion();
            int commandeId = preparerDonnees();
            ProduitService produits = new ProduitService();
            Categorie informatique = new CategorieService().findAll().stream()
                    .filter(c -> c.getCode().equals("DEMO_INFO")).findFirst().orElseThrow();

            System.out.println("\n=== Categories ===");
            new CategorieService().findAll().forEach(System.out::println);
            System.out.println("\n=== Tous les produits ===");
            produits.findAll().forEach(System.out::println);
            System.out.println("\n=== Produits par categorie : " + informatique.getLibelle() + " ===");
            produits.findByCategorie(informatique.getId()).forEach(System.out::println);
            System.out.println("\n=== Produits commandes du 01/03/2013 au 31/03/2013 inclus ===");
            produits.findCommandesEntreDates(Date.valueOf("2013-03-01"), Date.valueOf("2013-03-31"))
                    .forEach(System.out::println);
            System.out.println("\n=== Detail de la commande ===");
            produits.afficherProduitsCommande(commandeId, System.out);
            System.out.println("\n=== Prix strictement superieur a 100 DH (NamedQuery) ===");
            produits.findPrixSuperieur100().forEach(System.out::println);
        } finally {
            HibernateUtil.shutdown();
        }
    }

    private static void verifierConnexion() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.doWork(connection -> {
                try (var statement = connection.createStatement();
                     var rs = statement.executeQuery("SELECT DATABASE(), VERSION(), CURRENT_USER()")) {
                    rs.next();
                    System.out.printf("Connexion MySQL OK | Base : %s | Version : %s | Utilisateur : %s%n",
                            rs.getString(1), rs.getString(2), rs.getString(3));
                }
            });
        }
    }

    /** Initialisation atomique et reutilisable, sans doublons a chaque execution. */
    private static int preparerDonnees() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            try {
                Long dejaPresent = session.createQuery(
                        "select count(c) from Categorie c where c.code = 'DEMO_INFO'", Long.class)
                        .getSingleResult();
                if (dejaPresent > 0) {
                    Integer id = session.createQuery("""
                            select l.commande.id from LigneCommandeProduit l
                            where l.produit.reference = 'ZR85' and l.commande.date = :date
                            order by l.commande.id
                            """, Integer.class).setParameter("date", Date.valueOf("2013-03-14"))
                            .setMaxResults(1).uniqueResult();
                    if (id == null) throw new IllegalStateException("Donnees DEMO incompletes : verifier la base.");
                    transaction.commit();
                    return id;
                }

                Categorie info = new Categorie("DEMO_INFO", "Materiel informatique");
                Categorie accessoires = new Categorie("DEMO_ACC", "Accessoires");
                session.persist(info);
                session.persist(accessoires);
                Produit es12 = new Produit("ES12", 120f, info);
                Produit zr85 = new Produit("ZR85", 100f, info);
                Produit ee85 = new Produit("EE85", 200f, info);
                Produit souris = new Produit("SO80", 80f, accessoires);
                Produit clavier = new Produit("CL150", 150f, accessoires);
                for (Produit p : new Produit[]{es12, zr85, ee85, souris, clavier}) session.persist(p);

                Commande debut = new Commande(Date.valueOf("2013-03-01"));
                Commande fin = new Commande(Date.valueOf("2013-03-31"));
                Commande horsPeriode = new Commande(Date.valueOf("2013-04-01"));
                Commande exemple = new Commande(Date.valueOf("2013-03-14"));
                for (Commande c : new Commande[]{debut, fin, horsPeriode, exemple}) session.persist(c);
                session.persist(new LigneCommandeProduit(debut, es12, 2));
                session.persist(new LigneCommandeProduit(fin, souris, 3));
                session.persist(new LigneCommandeProduit(horsPeriode, clavier, 4));
                session.persist(new LigneCommandeProduit(exemple, es12, 7));
                session.persist(new LigneCommandeProduit(exemple, zr85, 14));
                session.persist(new LigneCommandeProduit(exemple, ee85, 5));
                transaction.commit();
                System.out.println("Creation : 2 categories, 5 produits, 4 commandes et 6 lignes.");
                return exemple.getId();
            } catch (RuntimeException e) {
                if (transaction.isActive()) transaction.rollback();
                throw e;
            }
        }
    }
}
