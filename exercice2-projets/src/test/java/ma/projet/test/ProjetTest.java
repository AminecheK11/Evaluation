package ma.projet.test;

import ma.projet.classes.*;
import ma.projet.service.*;
import ma.projet.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

/** Integration sur MySQL reel ; nettoyage limite aux identifiants de chaque test. */
@org.junit.jupiter.api.extension.ExtendWith(TestDatabaseExtension.class)
class ProjetTest {
    private final EmployeService employes = new EmployeService();
    private final ProjetService projets = new ProjetService();
    private final TacheService taches = new TacheService();
    private final EmployeTacheService affectations = new EmployeTacheService();
    private final List<Integer> employeIds = new ArrayList<>();
    private final List<Integer> projetIds = new ArrayList<>();
    private final List<Integer> tacheIds = new ArrayList<>();
    private Employe chef, collegue, autre;
    private Projet stock, portail, vide;
    private Tache analyse, conception, developpement, enCours, nonCommencee, horsPeriode, sansAffectation;
    private String prefixe;

    @BeforeEach
    void creerDonneesViaLesServices() {
        prefixe = "TEST_PROJET_" + UUID.randomUUID() + "_";
        chef = employe("Chef");
        collegue = employe("Collegue");
        autre = employe("Autre");
        stock = projet("Stock", chef);
        portail = projet("Portail", chef);
        vide = projet("Vide", collegue);
        analyse = tache("Analyse", 1500, stock);
        conception = tache("Conception", 1000, stock);
        developpement = tache("Developpement", 5000, stock);
        enCours = tache("En cours", 800, stock);
        nonCommencee = tache("Non commencee", 1200, stock);
        sansAffectation = tache("Sans affectation", 0, stock);
        horsPeriode = tache("Hors periode", 700, portail);
        affecter(chef, analyse, "2013-02-10", "2013-02-20");
        affecter(collegue, analyse, "2013-02-12", "2013-02-18");
        affecter(chef, conception, "2013-03-10", "2013-03-15");
        affecter(collegue, developpement, "2013-04-10", "2013-04-25");
        affecter(chef, enCours, "2013-02-15", null);
        affecter(collegue, nonCommencee, null, null);
        affecter(autre, horsPeriode, "2013-05-01", "2013-05-10");
    }

    private static Date d(String date) { return date == null ? null : Date.valueOf(date); }
    private Employe employe(String nom) {
        Employe e = new Employe(prefixe + nom, "Prenom", "0600000000");
        assertTrue(employes.create(e));
        employeIds.add(e.getId());
        return e;
    }
    private Projet projet(String nom, Employe responsable) {
        Projet p = new Projet(prefixe + nom, d("2013-01-14"), d("2013-06-30"), responsable);
        assertTrue(projets.create(p));
        projetIds.add(p.getId());
        return p;
    }
    private Tache tache(String nom, double prix, Projet projet) {
        // Dates planifiees volontairement differentes des dates reelles.
        Tache t = new Tache(prefixe + nom, d("2013-01-15"), d("2013-01-31"), prix, projet);
        assertTrue(taches.create(t));
        tacheIds.add(t.getId());
        return t;
    }
    private EmployeTache affecter(Employe employe, Tache tache, String debut, String fin) {
        EmployeTache et = new EmployeTache(employe, tache, d(debut), d(fin));
        assertTrue(affectations.create(et));
        return et;
    }
    private Set<Integer> ids(List<Tache> resultats) {
        return resultats.stream().filter(t -> tacheIds.contains(t.getId()))
                .map(Tache::getId).collect(Collectors.toSet());
    }

    @AfterEach
    void nettoyerUniquementCeTest() {
        if (employeIds.isEmpty() && projetIds.isEmpty() && tacheIds.isEmpty()) return;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                if (!tacheIds.isEmpty()) {
                    session.createQuery("delete from EmployeTache et where et.tache.id in :ids")
                            .setParameter("ids", tacheIds).executeUpdate();
                    session.createQuery("delete from Tache t where t.id in :ids")
                            .setParameter("ids", tacheIds).executeUpdate();
                }
                if (!projetIds.isEmpty()) session.createQuery("delete from Projet p where p.id in :ids")
                        .setParameter("ids", projetIds).executeUpdate();
                if (!employeIds.isEmpty()) session.createQuery("delete from Employe e where e.id in :ids")
                        .setParameter("ids", employeIds).executeUpdate();
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    @AfterAll
    static void fermer() { HibernateUtil.shutdown(); }

    @Test
    void connexionMySQLEtModeleIndependant() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.doWork(c -> {
                assertEquals("MySQL", c.getMetaData().getDatabaseProductName());
                assertEquals("evaluation_projets_test", c.getCatalog());
                assertTrue(c.isValid(5));
            });
            assertEquals(4, session.getSessionFactory().getMetamodel().getEntities().size());
            for (Class<?> type : List.of(Employe.class, Projet.class, Tache.class, EmployeTache.class)) {
                assertNotNull(session.getSessionFactory().getMetamodel().entity(type));
            }
        }
    }

    @Test
    void creationDesEntitesEtRelationsBidirectionnelles() {
        assertTrue(chef.getId() > 0 && stock.getId() > 0 && analyse.getId() > 0);
        assertEquals(chef.getNom(), employes.findById(chef.getId()).getNom());
        assertEquals(chef.getId(), projets.findById(stock.getId()).getChefProjet().getId());
        assertEquals(stock.getId(), taches.findById(analyse.getId()).getProjet().getId());
        EmployeTache et = affectations.findById(new EmployeTacheId(chef.getId(), analyse.getId()));
        assertEquals(d("2013-02-10"), et.getDateDebutReelle());
        assertEquals(d("2013-02-20"), et.getDateFinReelle());
        assertEquals(chef.getNom(), et.getEmploye().getNom());
        assertEquals(stock.getNom(), et.getTache().getProjet().getNom());
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            assertEquals(2, session.get(Employe.class, chef.getId()).getProjetsGeres().size());
            assertEquals(3, session.get(Employe.class, chef.getId()).getAffectations().size());
            assertEquals(6, session.get(Projet.class, stock.getId()).getTaches().size());
            assertEquals(2, session.get(Tache.class, analyse.getId()).getAffectations().size());
        }
    }

    @Test
    void tachesRealiseesParEmployeSansTachesEnCours() {
        assertEquals(Set.of(analyse.getId(), conception.getId()), ids(employes.findTachesRealisees(chef.getId())));
        assertEquals(Set.of(analyse.getId(), developpement.getId()), ids(employes.findTachesRealisees(collegue.getId())));
        assertTrue(employes.findTachesRealisees(-1).isEmpty());
    }

    @Test
    void projetsGeresParEmploye() {
        assertEquals(Set.of(stock.getId(), portail.getId()), employes.findProjetsGeres(chef.getId())
                .stream().map(Projet::getId).collect(Collectors.toSet()));
        assertEquals(List.of(vide.getId()), employes.findProjetsGeres(collegue.getId())
                .stream().map(Projet::getId).toList());
        assertTrue(employes.findProjetsGeres(autre.getId()).isEmpty());
        assertTrue(employes.findProjetsGeres(-1).isEmpty());
    }

    @Test
    void toutesLesTachesPlanifieesMemeSansRealisation() {
        assertEquals(Set.of(analyse.getId(), conception.getId(), developpement.getId(),
                enCours.getId(), nonCommencee.getId(), sansAffectation.getId()),
                ids(projets.findTachesPlanifiees(stock.getId())));
        assertTrue(projets.findTachesPlanifiees(vide.getId()).isEmpty());
        assertTrue(projets.findTachesPlanifiees(-1).isEmpty());
    }

    @Test
    void realisationsProjetConserventLesDatesDeChaqueEmploye() {
        List<EmployeTache> resultat = projets.findTachesRealisees(stock.getId());
        assertEquals(4, resultat.size());
        assertEquals(2, resultat.stream().filter(et -> et.getTache().getId() == analyse.getId()).count());
        assertTrue(resultat.stream().allMatch(et -> et.getDateDebutReelle() != null && et.getDateFinReelle() != null));
        EmployeTache ligne = resultat.stream().filter(et -> et.getTache().getId() == analyse.getId()
                && et.getEmploye().getId() == collegue.getId()).findFirst().orElseThrow();
        assertEquals(d("2013-02-12"), ligne.getDateDebutReelle());
        assertEquals(d("2013-02-18"), ligne.getDateFinReelle());
        assertTrue(projets.findTachesRealisees(vide.getId()).isEmpty());
    }

    @Test
    void namedQueryPrixStrictementSuperieur1000() {
        Tache limite = tache("1000.01", 1000.01, stock);
        Set<Integer> attendu = Set.of(analyse.getId(), developpement.getId(), nonCommencee.getId(), limite.getId());
        assertEquals(attendu, ids(taches.findPrixSuperieur1000()));
        assertFalse(ids(taches.findPrixSuperieur1000()).contains(conception.getId()));
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            assertEquals(attendu, ids(session.createNamedQuery("Tache.prixSuperieur1000", Tache.class).getResultList()));
        }
    }

    @Test
    void periodeSurDatesReellesBornesInclusivesEtSansDoublons() {
        List<Tache> resultat = taches.findRealiseesEntreDates(d("2013-02-10"), d("2013-04-25"));
        assertEquals(Set.of(analyse.getId(), conception.getId(), developpement.getId()), ids(resultat));
        assertEquals(1, resultat.stream().filter(t -> t.getId() == analyse.getId()).count());
        // Les dates planifiees sont en janvier, les realisations n'y sont pas.
        assertTrue(ids(taches.findRealiseesEntreDates(d("2013-01-01"), d("2013-01-31"))).isEmpty());
    }

    @Test
    void chevauchementPartielEtTachesNonTermineesExclus() {
        assertTrue(ids(taches.findRealiseesEntreDates(d("2013-03-11"), d("2013-03-14"))).isEmpty());
        assertTrue(ids(taches.findRealiseesEntreDates(d("2013-03-10"), d("2013-03-14"))).isEmpty());
        Set<Integer> resultat = ids(taches.findRealiseesEntreDates(d("2013-01-01"), d("2013-12-31")));
        assertFalse(resultat.contains(enCours.getId()));
        assertFalse(resultat.contains(nonCommencee.getId()));
        assertFalse(resultat.contains(sansAffectation.getId()));
        assertTrue(resultat.contains(horsPeriode.getId()));
    }

    @Test
    void realisationSurUnJourEtPeriodeVide() {
        Tache jour = tache("Un jour", 10, stock);
        affecter(autre, jour, "2013-03-10", "2013-03-10");
        assertEquals(Set.of(jour.getId()), ids(taches.findRealiseesEntreDates(d("2013-03-10"), d("2013-03-10"))));
        assertTrue(ids(taches.findRealiseesEntreDates(d("1990-01-01"), d("1990-01-31"))).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> taches.findRealiseesEntreDates(d("2013-03-20"), d("2013-03-10")));
        assertThrows(IllegalArgumentException.class,
                () -> taches.findRealiseesEntreDates(null, d("2013-03-10")));
    }

    @Test
    void affichageProjetAvecDatesReellesEtEmployes() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        projets.afficherTachesRealisees(stock.getId(), new PrintStream(buffer, true, StandardCharsets.UTF_8));
        String texte = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(texte.contains("Projet : " + stock.getId()));
        assertTrue(texte.contains("Nom : " + stock.getNom()));
        assertTrue(texte.contains("14 janvier 2013"));
        assertTrue(texte.contains("Date Début Réelle") && texte.contains("Date Fin Réelle"));
        assertTrue(texte.contains("10/02/2013") && texte.contains("20/02/2013"));
        assertTrue(texte.contains("10/03/2013") && texte.contains("15/03/2013"));
        assertTrue(texte.contains("10/04/2013") && texte.contains("25/04/2013"));
        assertTrue(texte.contains(chef.getNom()));
        assertFalse(texte.contains(enCours.getNom()));
        assertThrows(IllegalArgumentException.class, () -> projets.afficherTachesRealisees(-1, System.out));
    }

    @Test
    void coutGlobalSansDoubleComptageDesAffectations() {
        assertEquals(9500.0, projets.calculerCoutGlobal(stock.getId()), 0.001);
        assertEquals(700.0, projets.calculerCoutGlobal(portail.getId()), 0.001);
        assertEquals(0.0, projets.calculerCoutGlobal(vide.getId()), 0.001);
    }

    @Test
    void crudEmploye() {
        Employe e = employe("Temporaire");
        e.setNom(prefixe + "Modifie");
        e.setPrenom("Nouveau");
        e.setTelephone("0612345678");
        assertTrue(employes.update(e));
        Employe relu = employes.findById(e.getId());
        assertEquals(e.getNom(), relu.getNom());
        assertEquals("Nouveau", relu.getPrenom());
        assertEquals("0612345678", relu.getTelephone());
        assertTrue(employes.findAll().stream().anyMatch(x -> x.getId() == e.getId()));
        assertTrue(employes.delete(e));
        assertNull(employes.findById(e.getId()));
        assertFalse(employes.update(e));
        assertFalse(employes.delete(e));
    }

    @Test
    void crudProjet() {
        Projet p = projet("Temporaire", employes.findById(chef.getId()));
        p.setNom(prefixe + "Renomme");
        p.setChefProjet(employes.findById(autre.getId()));
        p.setDatesPlanifiees(d("2014-01-01"), d("2014-06-01"));
        assertTrue(projets.update(p));
        Projet relu = projets.findById(p.getId());
        assertEquals(p.getNom(), relu.getNom());
        assertEquals(autre.getId(), relu.getChefProjet().getId());
        assertEquals(p.getDateDebut(), relu.getDateDebut());
        assertEquals(p.getDateFin(), relu.getDateFin());
        assertTrue(projets.findAll().stream().anyMatch(x -> x.getId() == p.getId()));
        assertTrue(projets.delete(p));
        assertNull(projets.findById(p.getId()));
        assertFalse(projets.update(p));
        assertFalse(projets.delete(p));
    }

    @Test
    void crudTache() {
        Tache t = tache("Temporaire", 1, projets.findById(stock.getId()));
        t.setNom(prefixe + "Renommee");
        t.setPrix(1000.5);
        t.setDatesPlanifiees(d("2013-02-01"), d("2013-02-10"));
        t.setProjet(projets.findById(portail.getId()));
        assertTrue(taches.update(t));
        Tache relue = taches.findById(t.getId());
        assertEquals(t.getNom(), relue.getNom());
        assertEquals(1000.5, relue.getPrix());
        assertEquals(t.getDateDebut(), relue.getDateDebut());
        assertEquals(t.getDateFin(), relue.getDateFin());
        assertEquals(portail.getId(), relue.getProjet().getId());
        assertTrue(taches.findAll().stream().anyMatch(x -> x.getId() == t.getId()));
        assertTrue(taches.delete(t));
        assertNull(taches.findById(t.getId()));
        assertFalse(taches.update(t));
        assertFalse(taches.delete(t));
    }

    @Test
    void crudAffectationAvecParentsRelus() {
        EmployeTache et = affecter(employes.findById(autre.getId()),
                taches.findById(sansAffectation.getId()), null, null);
        EmployeTache relue = affectations.findById(et.getId());
        assertNull(relue.getDateDebutReelle());
        assertNull(relue.getDateFinReelle());
        relue.setDateDebutReelle(d("2013-03-01"));
        assertTrue(affectations.update(relue));
        assertFalse(ids(employes.findTachesRealisees(autre.getId())).contains(sansAffectation.getId()));
        relue.setDateFinReelle(d("2013-03-05"));
        assertTrue(affectations.update(relue));
        assertEquals(d("2013-03-05"), affectations.findById(et.getId()).getDateFinReelle());
        assertTrue(ids(employes.findTachesRealisees(autre.getId())).contains(sansAffectation.getId()));
        assertTrue(affectations.findAll().stream().anyMatch(x -> x.getId().equals(et.getId())));
        assertTrue(affectations.delete(relue));
        assertNull(affectations.findById(et.getId()));
        assertFalse(affectations.update(relue));
        assertFalse(affectations.delete(relue));
    }

    @Test
    void affectationDupliqueeRefuseeAvecRollback() {
        assertThrows(RuntimeException.class, () -> affecter(chef, analyse, "2013-03-01", "2013-03-10"));
        EmployeTache initiale = affectations.findById(new EmployeTacheId(chef.getId(), analyse.getId()));
        assertEquals(d("2013-02-10"), initiale.getDateDebutReelle());
        assertEquals(d("2013-02-20"), initiale.getDateFinReelle());
        assertEquals(4, projets.findTachesRealisees(stock.getId()).size());
    }

    @Test
    void parentsNonEnregistresRefuses() {
        Employe inconnu = new Employe("Inconnu", "Inconnu", "000");
        assertThrows(IllegalArgumentException.class,
                () -> affectations.create(new EmployeTache(inconnu, analyse, null, null)));
        Tache inconnue = new Tache("Inconnue", d("2013-01-01"), d("2013-01-02"), 1, stock);
        assertThrows(IllegalArgumentException.class,
                () -> affectations.create(new EmployeTache(chef, inconnue, null, null)));
    }

    @Test
    void datesEtPrixInvalidesRefuses() {
        assertThrows(IllegalArgumentException.class, () -> new EmployeTache(chef, analyse, null, d("2013-01-02")));
        assertThrows(IllegalArgumentException.class,
                () -> new EmployeTache(chef, analyse, d("2013-01-03"), d("2013-01-02")));
        assertThrows(IllegalArgumentException.class,
                () -> new Projet("Invalide", d("2013-01-03"), d("2013-01-02"), chef));
        assertThrows(IllegalArgumentException.class,
                () -> new Tache("Invalide", null, d("2013-01-02"), 1, stock));
        assertThrows(IllegalArgumentException.class, () -> analyse.setPrix(-1));
        assertThrows(IllegalArgumentException.class, () -> analyse.setPrix(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> analyse.setPrix(Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> stock.setChefProjet(null));
        assertThrows(IllegalArgumentException.class, () -> analyse.setProjet(null));
    }

    @Test
    void datesProtegeesDesModificationsExternes() {
        Date debut = d("2013-01-01");
        Date fin = d("2013-01-05");
        EmployeTache et = new EmployeTache(chef, analyse, debut, fin);
        debut.setTime(0);
        fin.setTime(0);
        et.getDateDebutReelle().setTime(0);
        et.getDateFinReelle().setTime(0);
        stock.getDateDebut().setTime(0);
        analyse.getDateFin().setTime(0);
        assertEquals(d("2013-01-01"), et.getDateDebutReelle());
        assertEquals(d("2013-01-05"), et.getDateFinReelle());
        assertEquals(d("2013-01-14"), stock.getDateDebut());
        assertEquals(d("2013-01-31"), analyse.getDateFin());
    }
    @Test
    void compositionSupprimeTachesEtAffectationsMaisGardeEmployes() {
        assertTrue(projets.delete(stock));
        assertNull(projets.findById(stock.getId()));
        assertNull(taches.findById(analyse.getId()));
        assertNull(affectations.findById(new EmployeTacheId(chef.getId(), analyse.getId())));
        assertNotNull(employes.findById(chef.getId()));
        assertNotNull(taches.findById(horsPeriode.getId()));
    }

    @Test
    void compositionRetraitTacheSupprimeOrphelinEtAffectations() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            Projet p = s.get(Projet.class, stock.getId());
            assertTrue(p.getTaches().removeIf(t -> t.getId() == analyse.getId()));
            tx.commit();
        }
        assertNull(taches.findById(analyse.getId()));
        assertNull(affectations.findById(new EmployeTacheId(chef.getId(), analyse.getId())));
        assertNotNull(projets.findById(stock.getId()));
    }
}
