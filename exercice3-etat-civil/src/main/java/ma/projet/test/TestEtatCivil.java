package ma.projet.test;

import ma.projet.beans.*;
import ma.projet.service.*;
import ma.projet.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.sql.Date;
import java.util.List;

public class TestEtatCivil {
    public static void main(String[] args) {
        try {
            preparerDonnees();
            FemmeService femmes = new FemmeService();
            HommeService hommes = new HommeService();
            List<Femme> fs = femmes.findAll();
            List<Homme> hs = hommes.findAll();
            if (fs.size() != 10 || hs.size() != 5)
                throw new IllegalStateException("La demonstration exige exactement 10 femmes et 5 hommes.");
            System.out.println("Base MySQL : evaluation_etat_civil");
            System.out.println("Démonstration : exactement 10 femmes et 5 hommes.");
            System.out.println("\n=== Liste des femmes ===");
            fs.forEach(System.out::println);
            System.out.println("\n=== Femme la plus âgée ===");
            System.out.println(femmes.findPlusAgee());
            System.out.println("\n=== Épouses entre le 01/01/2013 et le 31/12/2020 ===");
            hommes.afficherEpousesEntreDates(hs.get(0).getId(), d("2013-01-01"), d("2020-12-31"), System.out);
            System.out.println("\n=== Nombre d'enfants : requête native nommée ===");
            System.out.println(fs.get(4).getNom() + " " + fs.get(4).getPrenom() + " : "
                    + femmes.compterEnfantsEntreDates(fs.get(4).getId(), d("1999-01-01"), d("2020-12-31")) + " enfants");
            System.out.println("Somme sur les mariages actifs pendant la période 1999–2020.");
            System.out.println("\n=== Femmes mariées au moins deux fois : requête nommée ===");
            femmes.findMarieesAuMoinsDeuxFois().forEach(System.out::println);
            System.out.println("\n=== Hommes ayant quatre épouses simultanées entre 2013 et 2020 : Criteria ===");
            hommes.findHommesMariesAQuatreFemmesEntreDates(d("2013-01-01"), d("2020-12-31")).forEach(System.out::println);
            System.out.println("Nombre : " + hommes.compterHommesMariesAQuatreFemmesEntreDates(d("2013-01-01"), d("2020-12-31")));
            System.out.println("\n=== Détail des mariages ===");
            hommes.afficherMariages(hs.get(0).getId(), System.out);
        } finally { HibernateUtil.shutdown(); }
    }

    private static Date d(String date) { return date == null ? null : Date.valueOf(date); }
    private static void preparerDonnees() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                long personnes = s.createQuery("select count(p) from Personne p", Long.class).getSingleResult();
                if (personnes != 0) {
                    long femmes = s.createQuery("select count(f) from Femme f", Long.class).getSingleResult();
                    long hommes = s.createQuery("select count(h) from Homme h", Long.class).getSingleResult();
                    if (femmes != 10 || hommes != 5)
                        throw new IllegalStateException("Base non vide : ne pas ajouter un second jeu de demonstration.");
                    tx.commit();
                    System.out.println("Données existantes réutilisées sans ajout.");
                    return;
                }
                String[] prenomsF = {"Amina","Salma","Nadia","Lina","Fatima","Sara","Hana","Leila","Meryem","Imane"};
                String[] prenomsH = {"Ahmed","Omar","Youssef","Karim","Ali"};
                Femme[] f = new Femme[10];
                Homme[] h = new Homme[5];
                for (int i = 0; i < 10; i++) {
                    f[i] = new Femme("F" + (i + 1), prenomsF[i], "06000001" + String.format("%02d", i),
                            "Rabat, adresse " + (i + 1), d((1950 + i * 2) + "-01-15"));
                    s.persist(f[i]);
                }
                for (int i = 0; i < 5; i++) {
                    h[i] = new Homme("H" + (i + 1), prenomsH[i], "06000002" + String.format("%02d", i),
                            "Casablanca, adresse " + (i + 1), d((1945 + i * 2) + "-06-10"));
                    s.persist(h[i]);
                }
                for (int i = 0; i < 4; i++) s.persist(new Mariage(h[0], f[i], d((2010 + i) + "-01-01"), null, i + 1));
                s.persist(new Mariage(h[0], f[4], d("2000-01-01"), d("2005-12-31"), 2));
                s.persist(new Mariage(h[1], f[4], d("2006-01-01"), d("2009-12-31"), 3));
                s.persist(new Mariage(h[2], f[4], d("2010-01-01"), d("2011-12-31"), 1));
                s.persist(new Mariage(h[1], f[5], d("2010-01-01"), d("2012-12-31"), 1));
                s.persist(new Mariage(h[1], f[5], d("2014-01-01"), null, 2));
                for (int i = 0; i < 4; i++) s.persist(new Mariage(h[3], f[6 + i],
                        d((2010 + i * 2) + "-01-01"), d((2011 + i * 2) + "-12-31"), 0));
                tx.commit();
            } catch (RuntimeException e) { if (tx.isActive()) tx.rollback(); throw e; }
        }
    }
}
