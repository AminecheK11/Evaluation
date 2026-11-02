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

/** Tests d'integration sur le vrai serveur MySQL, sans base de substitution. */
@org.junit.jupiter.api.extension.ExtendWith(TestDatabaseExtension.class)
class StockTest {
    private final CategorieService categories = new CategorieService();
    private final ProduitService produits = new ProduitService();
    private final CommandeService commandes = new CommandeService();
    private final LigneCommandeService lignes = new LigneCommandeService();
    private final List<Integer> categorieIds = new ArrayList<>();
    private final List<Integer> produitIds = new ArrayList<>();
    private final List<Integer> commandeIds = new ArrayList<>();
    private Categorie info, accessoires;
    private Produit p120, p100, p200, p80, p150;
    private Commande debut, exemple, fin, horsPeriode;
    private String prefixe;

    @BeforeEach
    void creerDonneesParLesServices() {
        prefixe = "TEST_" + UUID.randomUUID() + "_";
        info = categorie("INFO");
        accessoires = categorie("ACC");
        p120 = produit("ES12", 120, info);
        p100 = produit("ZR85", 100, info);
        p200 = produit("EE85", 200, info);
        p80 = produit("SO80", 80, accessoires);
        p150 = produit("CL150", 150, accessoires);
        debut = commande("2013-03-01");
        exemple = commande("2013-03-14");
        fin = commande("2013-03-31");
        horsPeriode = commande("2013-04-01");
        assertTrue(lignes.create(new LigneCommandeProduit(debut, p120, 2)));
        assertTrue(lignes.create(new LigneCommandeProduit(exemple, p120, 7)));
        assertTrue(lignes.create(new LigneCommandeProduit(exemple, p100, 14)));
        assertTrue(lignes.create(new LigneCommandeProduit(exemple, p200, 5)));
        assertTrue(lignes.create(new LigneCommandeProduit(fin, p80, 3)));
        assertTrue(lignes.create(new LigneCommandeProduit(horsPeriode, p150, 4)));
    }

    private Categorie categorie(String suffixe) {
        Categorie c = new Categorie(prefixe + suffixe, suffixe);
        assertTrue(categories.create(c));
        categorieIds.add(c.getId());
        return c;
    }
    private Produit produit(String reference, float prix, Categorie categorie) {
        Produit p = new Produit(prefixe + reference, prix, categorie);
        assertTrue(produits.create(p));
        produitIds.add(p.getId());
        return p;
    }
    private Commande commande(String date) {
        Commande c = new Commande(Date.valueOf(date));
        assertTrue(commandes.create(c));
        commandeIds.add(c.getId());
        return c;
    }

    @AfterEach
    void supprimerUniquementLesDonneesDeCeTest() {
        if (categorieIds.isEmpty() && produitIds.isEmpty() && commandeIds.isEmpty()) return;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                if (!commandeIds.isEmpty()) {
                    session.createQuery("delete from LigneCommandeProduit l where l.commande.id in :ids")
                            .setParameter("ids", commandeIds).executeUpdate();
                    session.createQuery("delete from Commande c where c.id in :ids")
                            .setParameter("ids", commandeIds).executeUpdate();
                }
                if (!produitIds.isEmpty()) session.createQuery("delete from Produit p where p.id in :ids")
                        .setParameter("ids", produitIds).executeUpdate();
                if (!categorieIds.isEmpty()) session.createQuery("delete from Categorie c where c.id in :ids")
                        .setParameter("ids", categorieIds).executeUpdate();
                tx.commit();
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    @AfterAll
    static void fermerHibernate() { HibernateUtil.shutdown(); }

    private Set<Integer> ids(List<Produit> liste) {
        // La demonstration peut deja exister : on ne garde que les donnees du test courant.
        return liste.stream().filter(p -> produitIds.contains(p.getId()))
                .map(Produit::getId).collect(Collectors.toSet());
    }

    @Test
    void connexionAuVraiMySQL() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            session.doWork(connection -> {
                assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
                assertEquals("evaluation_stock_test", connection.getCatalog());
                assertTrue(connection.isValid(5));
            });
        }
    }

    @Test
    void creationDesQuatreEntitesEtRelations() {
        assertTrue(info.getId() > 0 && p120.getId() > 0 && exemple.getId() > 0);
        assertEquals(prefixe + "INFO", categories.findById(info.getId()).getCode());
        assertEquals(120f, produits.findById(p120.getId()).getPrix());
        assertEquals(exemple.getDate(), commandes.findById(exemple.getId()).getDate());
        LigneCommandeProduit ligne = lignes.findById(new LigneCommandeProduitId(exemple.getId(), p120.getId()));
        assertNotNull(ligne);
        assertEquals(7, ligne.getQuantite());
        assertEquals(info.getId(), ligne.getProduit().getCategorie().getId());
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            assertEquals(3, session.get(Categorie.class, info.getId()).getProduits().size());
            assertEquals(3, session.get(Commande.class, exemple.getId()).getLignesCommande().size());
            assertEquals(2, session.get(Produit.class, p120.getId()).getLignesCommande().size());
        }
    }

    @Test
    void produitsParCategorie() {
        assertEquals(Set.of(p120.getId(), p100.getId(), p200.getId()),
                ids(produits.findByCategorie(info.getId())));
        assertTrue(produits.findByCategorie(-1).isEmpty());
    }

    @Test
    void produitsEntreDatesInclusivesSansDoublons() {
        List<Produit> resultat = produits.findCommandesEntreDates(Date.valueOf("2013-03-01"), Date.valueOf("2013-03-31"));
        assertEquals(Set.of(p120.getId(), p100.getId(), p200.getId(), p80.getId()), ids(resultat));
        assertEquals(1, resultat.stream().filter(p -> p.getId() == p120.getId()).count());
        assertFalse(ids(resultat).contains(p150.getId()));
    }

    @Test
    void rechercheSurUnJourEtPeriodeVide() {
        assertEquals(Set.of(p120.getId(), p100.getId(), p200.getId()),
                ids(produits.findCommandesEntreDates(Date.valueOf("2013-03-14"), Date.valueOf("2013-03-14"))));
        assertTrue(ids(produits.findCommandesEntreDates(Date.valueOf("1990-01-01"), Date.valueOf("1990-01-31"))).isEmpty());
    }

    @Test
    void intervalleInvalideRefuse() {
        assertThrows(IllegalArgumentException.class,
                () -> produits.findCommandesEntreDates(Date.valueOf("2013-04-01"), Date.valueOf("2013-03-01")));
        assertThrows(IllegalArgumentException.class,
                () -> produits.findCommandesEntreDates(null, Date.valueOf("2013-03-01")));
    }

    @Test
    void produitsDuneCommandeAvecQuantites() {
        Map<Integer, Integer> quantites = produits.findByCommande(exemple.getId()).stream()
                .collect(Collectors.toMap(l -> l.getProduit().getId(), LigneCommandeProduit::getQuantite));
        assertEquals(Map.of(p120.getId(), 7, p100.getId(), 14, p200.getId(), 5), quantites);
        assertTrue(produits.findByCommande(-1).isEmpty());
    }

    @Test
    void affichageCommandeAvecDatePrixEtQuantites() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        produits.afficherProduitsCommande(exemple.getId(), new PrintStream(buffer, true, StandardCharsets.UTF_8));
        String texte = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(texte.contains("Commande : " + exemple.getId()));
        assertTrue(texte.contains("14 mars 2013"));
        assertTrue(texte.contains("Référence") && texte.contains("Quantité"));
        assertTrue(texte.matches("(?s).*ES12\\s+120 DH\\s+7.*"));
        assertTrue(texte.matches("(?s).*ZR85\\s+100 DH\\s+14.*"));
        assertTrue(texte.matches("(?s).*EE85\\s+200 DH\\s+5.*"));
        assertThrows(IllegalArgumentException.class, () -> produits.afficherProduitsCommande(-1, System.out));
    }

    @Test
    void namedQueryPrixStrictementSuperieur100() {
        assertEquals(Set.of(p120.getId(), p200.getId(), p150.getId()), ids(produits.findPrixSuperieur100()));
        // Verifie aussi que la requete nommee est enregistree dans Hibernate.
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            assertEquals(Set.of(p120.getId(), p200.getId(), p150.getId()),
                    ids(session.createNamedQuery("Produit.prixSuperieur100", Produit.class).getResultList()));
        }
    }

    @Test
    void crudCategorie() {
        Categorie c = categorie("VIDE");
        c.setLibelle("Nouveau libelle");
        assertTrue(categories.update(c));
        assertEquals("Nouveau libelle", categories.findById(c.getId()).getLibelle());
        assertTrue(categories.findAll().stream().anyMatch(x -> x.getId() == c.getId()));
        assertTrue(categories.delete(c));
        assertNull(categories.findById(c.getId()));
        assertFalse(categories.update(c));
        assertFalse(categories.delete(c));
    }

    @Test
    void crudProduit() {
        Produit p = produit("TEMP", 30, accessoires);
        p.setPrix(45);
        assertTrue(produits.update(p));
        assertEquals(45f, produits.findById(p.getId()).getPrix());
        assertTrue(produits.findAll().stream().anyMatch(x -> x.getId() == p.getId()));
        assertTrue(produits.delete(p));
        assertNull(produits.findById(p.getId()));
    }

    @Test
    void crudCommande() {
        Commande c = commande("2014-01-01");
        c.setDate(Date.valueOf("2014-02-01"));
        assertTrue(commandes.update(c));
        assertEquals(c.getDate(), commandes.findById(c.getId()).getDate());
        assertTrue(commandes.findAll().stream().anyMatch(x -> x.getId() == c.getId()));
        assertTrue(commandes.delete(c));
        assertNull(commandes.findById(c.getId()));
    }

    @Test
    void crudLigneCommande() {
        LigneCommandeProduit l = lignes.findById(new LigneCommandeProduitId(exemple.getId(), p120.getId()));
        l.setQuantite(9);
        assertTrue(lignes.update(l));
        assertEquals(9, lignes.findById(l.getId()).getQuantite());
        assertTrue(lignes.findAll().stream().anyMatch(x -> x.getId().equals(l.getId())));
        assertTrue(lignes.delete(l));
        assertNull(lignes.findById(l.getId()));
    }

    @Test
    void doublonDeLigneRefuseEtTransactionAnnulee() {
        assertThrows(RuntimeException.class,
                () -> lignes.create(new LigneCommandeProduit(exemple, p120, 99)));
        assertEquals(7, lignes.findById(new LigneCommandeProduitId(exemple.getId(), p120.getId())).getQuantite());
        assertEquals(3, produits.findByCommande(exemple.getId()).size());
    }

    @Test
    void creationAvecParentsRelusEtAffichagePrixDecimal() {
        Produit p = produit("DECIMAL", 120.5f, categories.findById(info.getId()));
        LigneCommandeProduit l = new LigneCommandeProduit(
                commandes.findById(exemple.getId()), produits.findById(p.getId()), 2);
        assertTrue(lignes.create(l));
        assertEquals(2, lignes.findById(l.getId()).getQuantite());
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        produits.afficherProduitsCommande(exemple.getId(), new PrintStream(buffer, true, StandardCharsets.UTF_8));
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("120,5 DH"));
    }
    @Test
    void quantiteEtPrixInvalidesRefuses() {
        assertThrows(IllegalArgumentException.class, () -> new LigneCommandeProduit(exemple, p120, 0));
        assertThrows(IllegalArgumentException.class, () -> new LigneCommandeProduit(exemple, p120, -1));
        assertThrows(IllegalArgumentException.class, () -> p120.setPrix(-1));
        assertThrows(IllegalArgumentException.class, () -> p120.setPrix(Float.NaN));
    }
}
