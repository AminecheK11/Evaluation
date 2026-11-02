package ma.projet.test;
import org.junit.jupiter.api.extension.*;
import ma.projet.util.HibernateUtil;
public final class TestDatabaseExtension implements BeforeAllCallback {
    public void beforeAll(ExtensionContext context) {
        System.setProperty("evaluation.test","true");
        context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL).getOrComputeIfAbsent(
            "close-hibernate",key -> (ExtensionContext.Store.CloseableResource)()->{HibernateUtil.shutdown();System.clearProperty("evaluation.test");});
    }
}
