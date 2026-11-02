package ma.projet.dao;
import java.io.Serializable;
import java.util.List;
/** Identifiant Integer ou cle composee serialisable selon l'entite. */
public interface IDao<T> {
    boolean create(T entity);
    boolean update(T entity);
    boolean delete(T entity);
    T findById(Serializable id);
    List<T> findAll();
}
