package ma.projet.util;
import ma.projet.classes.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
public final class HibernateUtil {
    private static SessionFactory factory;
    private HibernateUtil() {}
    public static synchronized SessionFactory getSessionFactory() {
        if(factory==null || factory.isClosed()) {
            Properties p=new Properties();
            try(InputStream in=HibernateUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
                if(in==null) throw new IllegalStateException("Configuration absente.");
                p.load(in);
                Path local=Path.of(System.getProperty("evaluation.config.dir","."),"application-local.properties");
                if(Files.isRegularFile(local)) try(Reader reader=Files.newBufferedReader(local)) { p.load(reader); }
            } catch(IOException e) { throw new IllegalStateException("Configuration illisible.",e); }
            String password=System.getenv("EVALUATION_DB_PASSWORD");
            if(password!=null) p.setProperty("hibernate.connection.password",password);
            if(Boolean.getBoolean("evaluation.test")) {
                p.setProperty("hibernate.connection.url","jdbc:mysql://127.0.0.1:3306/evaluation_projets_test?createDatabaseIfNotExist=true&connectionTimeZone=UTC");
                p.setProperty("hibernate.hbm2ddl.auto","update");
            }
            factory=new Configuration().addProperties(p)
                .addAnnotatedClass(Employe.class).addAnnotatedClass(Projet.class).addAnnotatedClass(Tache.class).addAnnotatedClass(EmployeTache.class).buildSessionFactory();
        }
        return factory;
    }
    public static synchronized void shutdown() { if(factory!=null && !factory.isClosed()) factory.close(); }
}
