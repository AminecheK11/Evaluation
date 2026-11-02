package ma.projet.test;

import ma.projet.beans.*;
import ma.projet.service.*;
import ma.projet.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.junit.jupiter.api.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.extension.ExtendWith(TestDatabaseExtension.class)
class EtatCivilTest {
    private final FemmeService femmes = new FemmeService();
    private final HommeService hommes = new HommeService();
    private final MariageService mariages = new MariageService();
    private final List<Integer> personnesIds = new ArrayList<>();
    private final List<Integer> mariageIds = new ArrayList<>();
    private Femme[] f;
    private Homme[] h;
    private long nombreAvant;
    private String prefixe;
    private static Date d(String value) { return value == null ? null : Date.valueOf(value); }

    @BeforeEach
    void preparer() {
        nombreAvant = hommes.compterHommesMariesAQuatreFemmesEntreDates(d("2013-01-01"), d("2020-12-31"));
        prefixe = "TEST_" + UUID.randomUUID() + "_";
        f = new Femme[10]; h = new Homme[5];
        for (int i = 0; i < f.length; i++) {
            f[i] = new Femme(prefixe + "F" + i, "Prenom", "0600000000", "Rabat", d((1900 + i) + "-01-01"));
            assertTrue(femmes.create(f[i])); personnesIds.add(f[i].getId());
        }
        for (int i = 0; i < h.length; i++) {
            h[i] = new Homme(prefixe + "H" + i, "Prenom", "0600000001", "Casa", d("1880-01-01"));
            assertTrue(hommes.create(h[i])); personnesIds.add(h[i].getId());
        }
        for (int i = 0; i < 4; i++) mariage(h[0], f[i], (2010 + i) + "-01-01", null, i + 1);
        mariage(h[0], f[4], "2000-01-01", "2005-12-31", 2);
        mariage(h[1], f[4], "2006-01-01", "2009-12-31", 3);
        mariage(h[2], f[4], "2010-01-01", "2011-12-31", 1);
        mariage(h[1], f[5], "2010-01-01", "2012-12-31", 1);
        mariage(h[1], f[5], "2014-01-01", null, 2);
        for (int i = 0; i < 4; i++) mariage(h[3], f[6 + i],
                (2010 + i * 2) + "-01-01", (2011 + i * 2) + "-12-31", 0);
    }
    private Mariage mariage(Homme homme, Femme femme, String debut, String fin, int enfants) {
        Mariage m = new Mariage(homme, femme, d(debut), d(fin), enfants);
        assertTrue(mariages.create(m)); mariageIds.add(m.getId()); return m;
    }

    @AfterEach
    void nettoyerSeulementLeTest() {
        if (personnesIds.isEmpty()) return;
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = s.beginTransaction();
            try {
                s.createQuery("delete from Mariage m where m.homme.id in :ids or m.femme.id in :ids")
                        .setParameter("ids", personnesIds).executeUpdate();
                s.createQuery("delete from Personne p where p.id in :ids")
                        .setParameter("ids", personnesIds).executeUpdate();
                tx.commit();
            } catch (RuntimeException e) { if (tx.isActive()) tx.rollback(); throw e; }
        }
    }
    @AfterAll static void fermer() { HibernateUtil.shutdown(); }
    private Set<Integer> ids(List<? extends Personne> liste) {
        return liste.stream().filter(p -> personnesIds.contains(p.getId()))
                .map(Personne::getId).collect(Collectors.toSet());
    }

    @Test void connexionEtModeleIndependant() {
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            s.doWork(c -> {
                assertEquals("MySQL", c.getMetaData().getDatabaseProductName());
                assertEquals("evaluation_etat_civil_test", c.getCatalog());
                assertTrue(c.isValid(5));
            });
            assertEquals(4, s.getSessionFactory().getMetamodel().getEntities().size());
        }
    }
    @Test void creationExactementDixFemmesEtCinqHommes() {
        assertEquals(10, ids(femmes.findAll()).size());
        assertEquals(5, ids(hommes.findAll()).size());
        assertEquals(13, mariageIds.size());
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            assertInstanceOf(Femme.class, s.get(Personne.class, f[0].getId()));
            assertInstanceOf(Homme.class, s.get(Personne.class, h[0].getId()));
            assertEquals(5, s.get(Homme.class, h[0].getId()).getMariages().size());
            assertEquals(3, s.get(Femme.class, f[4].getId()).getMariages().size());
        }
    }
    @Test void femmePlusAgee() {
        Date minimum = femmes.findAll().stream().map(Femme::getDateNaissance).min(DateComparator.INSTANCE).map(x -> new Date(x.getTime())).orElseThrow();
        assertEquals(minimum, femmes.findPlusAgee().getDateNaissance());
    }
    // Comparator explicite sur java.util.Date car le modele utilise java.util.Date.
    private enum DateComparator implements Comparator<java.util.Date> {
        INSTANCE;
        public int compare(java.util.Date a, java.util.Date b) { return a.compareTo(b); }
    }
    @Test void epousesPendantPeriodeEtSansDoublons() {
        assertEquals(Set.of(f[0].getId(), f[1].getId(), f[2].getId(), f[3].getId()),
                ids(hommes.findEpousesEntreDates(h[0].getId(), d("2013-01-01"), d("2020-12-31"))));
        assertEquals(1, hommes.findEpousesEntreDates(h[1].getId(), d("2010-01-01"), d("2020-12-31")).size());
        assertTrue(hommes.findEpousesEntreDates(h[4].getId(), d("2013-01-01"), d("2020-12-31")).isEmpty());
    }
    @Test void epousesBornesInclusives() {
        assertEquals(Set.of(f[4].getId()), ids(hommes.findEpousesEntreDates(h[0].getId(), d("2005-12-31"), d("2005-12-31"))));
        assertTrue(hommes.findEpousesEntreDates(h[0].getId(), d("2006-01-01"), d("2006-01-01")).isEmpty());
    }
    @Test void nativeNommeeNombreEnfants() {
        assertEquals(6, femmes.compterEnfantsEntreDates(f[4].getId(), d("1999-01-01"), d("2020-12-31")));
        assertEquals(1, femmes.compterEnfantsEntreDates(f[4].getId(), d("2010-01-01"), d("2011-12-31")));
        assertEquals(2, femmes.compterEnfantsEntreDates(f[4].getId(), d("2005-12-31"), d("2005-12-31")));
        assertEquals(0, femmes.compterEnfantsEntreDates(-1, d("2010-01-01"), d("2020-12-31")));
        assertEquals(0, femmes.compterEnfantsEntreDates(f[4].getId(), d("2020-01-01"), d("2020-12-31")));
    }
    @Test void femmesMarieesDeuxFoisMemeCoupleInclus() {
        assertEquals(Set.of(f[4].getId(), f[5].getId()), ids(femmes.findMarieesAuMoinsDeuxFois()));
        try (Session s = HibernateUtil.getSessionFactory().openSession()) {
            assertEquals(Set.of(f[4].getId(), f[5].getId()),
                    ids(s.createNamedQuery("Femme.marieesAuMoinsDeuxFois", Femme.class).getResultList()));
        }
    }
    @Test void criteriaQuatreEpousesSimultanees() {
        assertEquals(Set.of(h[0].getId()), ids(hommes.findHommesMariesAQuatreFemmesEntreDates(d("2013-01-01"), d("2020-12-31"))));
        assertEquals(nombreAvant + 1, hommes.compterHommesMariesAQuatreFemmesEntreDates(d("2013-01-01"), d("2020-12-31")));
    }
    @Test void criteriaQuatreSuccessivesNeSontPasSimultanees() {
        assertFalse(ids(hommes.findHommesMariesAQuatreFemmesEntreDates(d("2010-01-01"), d("2020-12-31"))).contains(h[3].getId()));
    }
    @Test void criteriaTroisEtCinqNeSontPasQuatre() {
        assertFalse(ids(hommes.findHommesMariesAQuatreFemmesEntreDates(d("2012-01-01"), d("2012-12-31"))).contains(h[0].getId()));
        mariage(h[0], f[6], "2013-01-01", null, 0);
        assertFalse(ids(hommes.findHommesMariesAQuatreFemmesEntreDates(d("2013-01-01"), d("2020-12-31"))).contains(h[0].getId()));
    }
    @Test void criteriaPassageDeCinqAQuatreApresUneFin() {
        mariage(h[0], f[6], "2013-01-01", "2015-06-30", 0);
        assertFalse(ids(hommes.findHommesMariesAQuatreFemmesEntreDates(d("2015-06-30"), d("2015-06-30"))).contains(h[0].getId()));
        assertTrue(ids(hommes.findHommesMariesAQuatreFemmesEntreDates(d("2015-06-30"), d("2015-07-01"))).contains(h[0].getId()));
    }
    @Test void criteriaCompteFemmesDistinctesPasLignes() {
        mariage(h[0], f[0], "2014-01-01", null, 0);
        assertTrue(ids(hommes.findHommesMariesAQuatreFemmesEntreDates(d("2015-01-01"), d("2015-01-01"))).contains(h[0].getId()));
    }
    @Test void periodesInvalidesRefusees() {
        assertThrows(IllegalArgumentException.class, () -> hommes.findEpousesEntreDates(h[0].getId(), null, d("2020-01-01")));
        assertThrows(IllegalArgumentException.class, () -> femmes.compterEnfantsEntreDates(f[0].getId(), d("2020-01-01"), d("2010-01-01")));
        assertThrows(IllegalArgumentException.class, () -> hommes.compterHommesMariesAQuatreFemmesEntreDates(d("2020-01-01"), null));
    }
    @Test void affichageMariagesSeparesEtDetails() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        hommes.afficherMariages(h[0].getId(), new PrintStream(buffer, true, StandardCharsets.UTF_8));
        String texte = buffer.toString(StandardCharsets.UTF_8);
        int separation = texte.indexOf("Mariages échoués");
        assertTrue(separation > texte.indexOf("Mariages en cours"));
        assertTrue(texte.substring(0, separation).contains(f[0].getNom()));
        assertFalse(texte.substring(0, separation).contains(f[4].getNom()));
        assertTrue(texte.substring(separation).contains(f[4].getNom()));
        assertTrue(texte.contains("01/01/2000") && texte.contains("31/12/2005"));
        assertTrue(texte.contains("Enfants"));
        assertTrue(texte.matches("(?s).*31/12/2005\\s+2.*"));
        assertThrows(IllegalArgumentException.class, () -> hommes.afficherMariages(-1, System.out));
    }
    @Test void affichageEpousesEtHommeSansMariage() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream out = new PrintStream(buffer, true, StandardCharsets.UTF_8);
        hommes.afficherEpousesEntreDates(h[0].getId(), d("2013-01-01"), d("2020-12-31"), out);
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains(f[0].getNom()));
        buffer.reset();
        hommes.afficherMariages(h[4].getId(), out);
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("Aucun mariage."));
    }
    @Test void crudFemme() {
        Femme x = new Femme(prefixe + "ExtraF", "A", "0", "Adresse", d("1970-01-01"));
        assertTrue(femmes.create(x)); personnesIds.add(x.getId());
        x.setNom(prefixe + "Modifie"); x.setPrenom("B"); x.setAdresse("Nouvelle"); x.setTelephone("1"); x.setDateNaissance(d("1971-01-01"));
        assertTrue(femmes.update(x));
        Femme relue = femmes.findById(x.getId());
        assertEquals("Nouvelle", relue.getAdresse()); assertEquals("B", relue.getPrenom());
        assertEquals("1", relue.getTelephone()); assertEquals(x.getNom(), relue.getNom());
        assertEquals(d("1971-01-01"), relue.getDateNaissance());
        assertTrue(femmes.delete(x)); assertNull(femmes.findById(x.getId()));
        assertFalse(femmes.update(x)); assertFalse(femmes.delete(x));
    }
    @Test void crudHomme() {
        Homme x = new Homme(prefixe + "ExtraH", "A", "0", "Adresse", d("1970-01-01"));
        assertTrue(hommes.create(x)); personnesIds.add(x.getId());
        x.setNom(prefixe + "Modifie");
        assertTrue(hommes.update(x)); assertEquals(x.getNom(), hommes.findById(x.getId()).getNom());
        assertTrue(hommes.delete(x)); assertNull(hommes.findById(x.getId()));
        assertFalse(hommes.update(x)); assertFalse(hommes.delete(x));
    }
    @Test void crudMariageParentsRelusEtRemariage() {
        Mariage m = mariage(hommes.findById(h[4].getId()), femmes.findById(f[9].getId()), "2021-01-01", null, 0);
        m.setNbrEnfant(2); m.setDateFin(d("2022-01-01"));
        assertTrue(mariages.update(m));
        Mariage relu = mariages.findById(m.getId());
        assertEquals(2, relu.getNbrEnfant()); assertEquals(d("2022-01-01"), relu.getDateFin());
        assertEquals(f[9].getNom(), relu.getFemme().getNom());
        Mariage second = mariage(h[4], f[9], "2023-01-01", null, 1);
        assertNotEquals(m.getId(), second.getId());
        assertTrue(mariages.findAll().stream().anyMatch(x -> x.getId() == m.getId()));
        assertTrue(mariages.delete(m)); assertNull(mariages.findById(m.getId()));
        assertFalse(mariages.update(m)); assertFalse(mariages.delete(m));
    }
    @Test void doublonExactRefuseEtRollback() {
        assertThrows(RuntimeException.class, () -> mariages.create(new Mariage(h[0], f[0], d("2010-01-01"), null, 99)));
        assertEquals(5, mariages.findByHomme(h[0].getId()).size());
        assertEquals(1, mariages.findById(mariageIds.get(0)).getNbrEnfant());
    }
    @Test void validationEtCopiesDesDates() {
        assertThrows(IllegalArgumentException.class, () -> new Mariage(h[0], f[0], null, null, 0));
        assertThrows(IllegalArgumentException.class, () -> new Mariage(h[0], f[0], d("2011-01-01"), d("2010-01-01"), 0));
        assertThrows(IllegalArgumentException.class, () -> new Mariage(h[0], f[0], d("2011-01-01"), null, -1));
        assertThrows(IllegalArgumentException.class, () -> f[0].setDateNaissance(null));
        Date debut = d("2010-01-01");
        Mariage m = new Mariage(h[0], f[0], debut, null, 0);
        debut.setTime(0); m.getDateDebut().setTime(0); f[0].getDateNaissance().setTime(0);
        assertEquals(d("2010-01-01"), m.getDateDebut());
        assertEquals(d("1900-01-01"), f[0].getDateNaissance());
    }
}
