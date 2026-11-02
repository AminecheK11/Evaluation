package ma.projet.test;

import ma.projet.classes.*;
import ma.projet.service.*;
import ma.projet.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

/** Demonstration de l'exercice 2, reutilisable sans doublons. */
public class TestProjet {
    public static void main(String[] args) {
        try {
            verifierConnexion();
            int projetId = preparerDonnees();
            ProjetService projets = new ProjetService();
            EmployeService employes = new EmployeService();
            TacheService taches = new TacheService();
            Projet projet = projets.findById(projetId);
            int chefId = projet.getChefProjet().getId();
            Employe chef = employes.findById(chefId);

            System.out.println("\n=== Employés ===");
            employes.findAll().forEach(System.out::println);
            System.out.println("\n=== Projets gérés par " + chef.getNom() + " " + chef.getPrenom() + " ===");
            employes.findProjetsGeres(chefId).forEach(System.out::println);
            System.out.println("\n=== Tâches réalisées par " + chef.getNom() + " " + chef.getPrenom() + " ===");
            employes.findTachesRealisees(chefId).forEach(System.out::println);
            System.out.println("\n=== Tâches planifiées : Gestion de stock ===");
            SimpleDateFormat date = new SimpleDateFormat("dd/MM/yyyy", Locale.FRENCH);
            System.out.printf("%-6s %-24s %-14s %-14s %s%n", "Num", "Nom", "Début prévu", "Fin prévue", "Prix");
            for (Tache t : projets.findTachesPlanifiees(projetId)) {
                System.out.printf(Locale.FRANCE, "%-6d %-24s %-14s %-14s %.2f DH%n",
                        t.getId(), t.getNom(), date.format(t.getDateDebut()), date.format(t.getDateFin()), t.getPrix());
            }
            System.out.println("\n=== Tâches réalisées et dates réelles ===");
            projets.afficherTachesRealisees(projetId, System.out);
            System.out.println("\n=== Tâches dont le prix > 1000 DH (NamedQuery) ===");
            taches.findPrixSuperieur1000().forEach(System.out::println);
            System.out.println("\n=== Tâches réalisées du 10/02/2013 au 25/04/2013 inclus ===");
            taches.findRealiseesEntreDates(Date.valueOf("2013-02-10"), Date.valueOf("2013-04-25"))
                    .forEach(System.out::println);
            System.out.println("Période appliquée aux dates réelles ; tâches terminées uniquement.");
            System.out.printf(Locale.FRANCE, "%nCoût global planifié du projet : %.2f DH%n",
                    projets.calculerCoutGlobal(projetId));
        } finally {
            HibernateUtil.shutdown();
        }
    }

    private static void verifierConnexion() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.doWork(connection -> {
                try (var statement = connection.createStatement();
                     var result = statement.executeQuery("SELECT DATABASE(), VERSION()")) {
                    result.next();
                    System.out.printf("Connexion MySQL OK | Base : %s | Version : %s%n",
                            result.getString(1), result.getString(2));
                }
            });
        }
    }

    private static int preparerDonnees() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                Integer existant = session.createQuery("""
                        select p.id from Projet p
                        where p.nom = 'Gestion de stock'
                        and p.chefProjet.telephone = '0600000201'
                        order by p.id
                        """, Integer.class).setMaxResults(1).uniqueResult();
                if (existant != null) {
                    tx.commit();
                    System.out.println("Données de démonstration existantes réutilisées.");
                    return existant;
                }
                Employe sara = new Employe("Alaoui", "Sara", "0600000201");
                Employe youssef = new Employe("Bennani", "Youssef", "0600000202");
                Employe lina = new Employe("Idrissi", "Lina", "0600000203");
                for (Employe e : new Employe[]{sara, youssef, lina}) session.persist(e);
                Projet stock = new Projet("Gestion de stock", d("2013-01-14"), d("2013-06-30"), sara);
                Projet portail = new Projet("Portail client", d("2013-01-01"), d("2013-12-31"), sara);
                Projet audit = new Projet("Audit informatique", d("2013-01-01"), d("2013-12-31"), youssef);
                for (Projet p : new Projet[]{stock, portail, audit}) session.persist(p);
                Tache analyse = new Tache("Analyse", d("2013-01-15"), d("2013-01-31"), 1500, stock);
                Tache conception = new Tache("Conception", d("2013-02-01"), d("2013-02-28"), 1000, stock);
                Tache dev = new Tache("Développement", d("2013-03-01"), d("2013-03-31"), 5000, stock);
                Tache integration = new Tache("Intégration", d("2013-04-01"), d("2013-04-30"), 800, stock);
                Tache documentation = new Tache("Documentation", d("2013-05-01"), d("2013-05-31"), 1200, stock);
                Tache maintenance = new Tache("Maintenance portail", d("2013-02-01"), d("2013-02-28"), 700, portail);
                for (Tache t : new Tache[]{analyse, conception, dev, integration, documentation, maintenance}) session.persist(t);
                session.persist(new EmployeTache(youssef, analyse, d("2013-02-10"), d("2013-02-20")));
                session.persist(new EmployeTache(sara, conception, d("2013-03-10"), d("2013-03-15")));
                session.persist(new EmployeTache(lina, dev, d("2013-04-10"), d("2013-04-25")));
                session.persist(new EmployeTache(lina, integration, d("2013-04-20"), null));
                session.persist(new EmployeTache(youssef, documentation, null, null));
                session.persist(new EmployeTache(sara, maintenance, d("2013-05-01"), d("2013-05-10")));
                session.persist(new EmployeTache(youssef, maintenance, d("2013-05-02"), d("2013-05-09")));
                tx.commit();
                System.out.println("Création : 3 employés, 3 projets, 6 tâches, 7 affectations.");
                return stock.getId();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    private static Date d(String value) { return Date.valueOf(value); }
}
