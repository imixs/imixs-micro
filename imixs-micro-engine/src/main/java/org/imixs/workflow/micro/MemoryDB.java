package org.imixs.workflow.micro;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.imixs.workflow.ItemCollection;

/**
 * In Memory DB.
 * <p>
 * The MemoryDB supports full concurrency of retrievals and updates of
 * ItemCollection objects.
 * The MemoryDB is based on the ConcurrentHashMap providing the same functional
 * specification
 * as a Hashtable, and includes versions of methods corresponding to each method
 * of Hashtable. However, even though all operations are thread-safe, retrieval
 * operations do not entail locking, and there is not any support for locking
 * the entire table in a way that prevents all access. Find details in
 * {@link ConcurrentHashMap}.
 * 
 */
public class MemoryDB {

    protected Map<String, ItemCollection> database = null;

    public static final String ISAUTHOR = "$isAuthor";
    public static final String NOINDEX = "$noindex";
    public static final String IMMUTABLE = "$immutable";

    /**
     * Constructor inits the database
     */
    public MemoryDB() {
        init();
    }

    /**
     * Create a test database with some workItems and a simple model
     */
    protected void init() {
        database = new ConcurrentHashMap<>();
    }

    /**
     * Loads a data element form the database
     * 
     * @param id - uniqueId of the ItemCollection
     * @return ItemCollection
     */
    public ItemCollection load(String id) {

        ItemCollection result = database.get(id);
        if (result != null) {
            // set author access=true
            result.replaceItemValue(ISAUTHOR, true);
        }
        return result;
    }

    /**
     * Stores a data element in the database
     * 
     * @param data - ItemCollection
     * @return return data instance.
     */
    public ItemCollection save(ItemCollection data) {
        if (data != null) {
            database.put(data.getUniqueID(), data);
        }
        return data;
    }

    /**
     * Removes a data element from the database
     * 
     * @param data - ItemCollection
     * 
     */
    public void delete(ItemCollection data) {
        if (data != null) {
            database.remove(data.getUniqueID());
        }
    }

}
