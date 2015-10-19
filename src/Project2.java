import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Austin Schey
 * CS 360
 * Project 2
 * 10/18/2015
 */

public class Project2 {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Error: invalid number of command" +
                    " line arguments. Please specify one filename.");
            System.exit(1);
        }
        String filename = args[0];
        HashTable table = new HashTable();
        // Read data from the file into the table
        readData(filename, table);
        // Print the values read into the table
        table.printValues();
    }

    private static void readData(String filename, HashTable table) {
        try (Scanner scan = new Scanner(new File(filename))) {
            // Split on all non-letter characters
            scan.useDelimiter("[\\s0-9,.!?\"$%&]");

            while (scan.hasNext()) {
                String word = scan.next();
                // Don't insert empty words
                if (word.isEmpty()) {
                    continue;
                }
                table.insert(word);
            }
        }
        catch (FileNotFoundException ex) {
            System.out.println("Error: file not found");
            System.exit(1);
        }
    }
}

class HashTable {
    private EntryList[] table;
    private int numEntries;

    public HashTable() {
        /**
         * I decided to implement the hash table with chaining
         * rather than open addressing because re-insertions
         * are faster when growing the table since the whole list
         * of items with the same hash can be inserted at once
         * instead of having to rehash each one separately.
         *
         * I tested the table with chaining and open addressing
         * and founded that chaining was significantly faster
         * in this case.
         *
         */
        this.table = new EntryList[1];
        this.numEntries = 0;

        // Generate a random seed for the hash function
        // This creates behavior akin to universal hashing
        MurmurHash3.seed();
    }

    private void rehash(EntryList list) {
        /**
         * Reinserts a list into the hash table after resizing
         *
         * Every word doesn't need to be rehashed separately because
         * they will all rehash to the same value regardless of the
         * table size
         *
         * This is one advantage to using chaining because every collision
         * would need to be rehashed with open addressing
         */
        int hash = this.generateHash(list.getHeadValue().getKey());
        this.table[hash] = list;
    }

    private int generateHash(String key) {
        /**
         * Calculates the hash value and returns it, adjusted for
         * the table size
         */
        return MurmurHash3.createHash(key) % this.table.length;
    }

    private EntryList createList(String key) {
        /**
         * Creates a new linked list with the given key
         */
        EntryList newList = new EntryList();
        newList.add(new HashTableEntry(key));
        return newList;
    }

    private HashTableEntry find(String key, int hash) {
        /**
         * Returns the HashTableEntry object with the given key
         * if it exists in the table
         */
        if (this.table[hash] == null) {
            return null;
        }
        EntryList entry = this.table[hash];
        return entry.find(key);
    }

    private void growTable() {
        /**
         * Multiplies the size of the table by 2 and rehashes
         * each list
         */
        EntryList[] tableCopy = this.deepCopy();
        this.table = new EntryList[this.table.length * 2];

        for (EntryList list : tableCopy) {
            if (list != null) {
                this.rehash(list);
            }
        }
    }

    private EntryList[] deepCopy() {
        /**
         * Returns a non-aliased copy of the table
         */
        EntryList[] newTable = new EntryList[this.table.length];
        for (int i = 0; i < this.table.length; i++) {
            newTable[i] = this.table[i];
        }
        return newTable;
    }

    public void insert(String key) {
        /**
         * Inserts a key into the hash table, resizing the table if needed
         *
         * I chose to grow the table when the load factor is above 0.7
         * because that seems to be a common choice. There does not seem to be a
         * consensus on when the best time to grow the table is.
         */
        // Grow the table if the load factor is greater than 0.7
        if ((double)this.numEntries / this.table.length > 0.7) {
            this.growTable();
        }
        // Use the lower case version of the string as the key to
        // store all the variants
        String lowerKey = key.toLowerCase();
        int hash = this.generateHash(lowerKey);
        // Check if the word has already been inserted
        HashTableEntry existingEntry = this.find(lowerKey, hash);
        if (existingEntry != null) {
            // If it has, increment the counter and add the key to the
            // list of variants
            existingEntry.addValue(key);
            return;
        }

        // If the slot is empty, create a new list
        if (this.table[hash] == null) {
            this.table[hash] = this.createList(key);
        }
        else {
            // Else, add a node to the list
            this.table[hash].add(new HashTableEntry(key));
        }

        this.numEntries++;
    }

    public void printValues() {
        /**
         * Prints out the table
         */
        System.out.println("Words " + this.numEntries);
        System.out.println();
        for (EntryList list : this.table) {
            if (list != null) {
                System.out.println(list);
            }
        }
    }
}

class HashTableEntry {
    private String key;
    private int value;
    private StringList variations;
    private boolean printVariations;

    public HashTableEntry(String key) {
        this.key = key.toLowerCase();
        this.value = 0;
        this.variations = new StringList();
        this.addValue(key);
    }

    public String getKey() {
        /**
         * Returns the key associated with this entry
         */
        return this.key;
    }

    public void addValue(String variation) {
        /**
         * Adds the variation to the list if it does not already exist
         * and increments the count of the number of words added
         */
        this.value++;
        if (!this.variations.contains(variation)) {
            this.variations.add(variation);
        }
        // If there is at least one variation that's different than the key,
        // print the list of variations
        if (!(variation.equals(this.key))) {
            this.printVariations = true;
        }
    }

    @Override
    public String toString() {
        /**
         * Returns a string representation of the object
         */
        if (this.printVariations) {
            return key + " (" + this.variations + ") - " + this.value;
        }
        return key + " - " + this.value;
    }
}

class Node<T> {
    Node<T> next;
    T value;

    public Node(T value) {
        this.value = value;
    }
}

abstract class LinkedList<T> {
    Node<T> sentinel;
    int numNodes;

    public LinkedList() {
        // Use a sentinel to avoid null checking
        this.sentinel = new Node<>(null);
        this.numNodes = 0;
    }

    public void add(T value) {
        /**
         * Adds the node to the front of the list
         */
        Node<T> newNode = new Node<>(value);
        Node<T> newNext = this.sentinel.next;
        this.sentinel.next = newNode;
        newNode.next = newNext;
        this.numNodes++;
    }

    public abstract Object find(String key);

    public boolean contains(String key) {
        /**
         * Returns true if the list contains the key
         */
        return this.find(key) != null;
    }

    public T getHeadValue() {
        /**
         * Returns the first value in the list
         */
        return this.sentinel.next.value;
    }
}

class StringList extends LinkedList<String> {
    @Override
    public String toString() {
        /**
         * Returns a string representation of the object
         */
        Node current = this.sentinel.next;
        String result = current.value.toString();
        for (int i = 0; i < this.numNodes - 1; i++) {
            current = current.next;
            result += (" " + current.value.toString());
        }
        return result;
    }

    @Override
    public String find(String key) {
        /**
         * Finds the value in the list that matches the key
         */
        Node<String> current = this.sentinel.next;
        for (int i = 0; i < this.numNodes; i++) {
            if (current.value.equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }
}

class EntryList extends LinkedList<HashTableEntry> {
    @Override
    public String toString() {
        /**
         * Returns a string representation of the object
         */
        Node current = this.sentinel.next;
        String result = current.value.toString();
        for (int i = 0; i < this.numNodes - 1; i++) {
            current = current.next;
            result += ("\n" + current.value.toString());
        }
        return result;
    }

    @Override
    public HashTableEntry find(String key) {
        /**
         * Returns the entry with the given key
         */
        Node<HashTableEntry> current = this.sentinel.next;
        for (int i = 0; i < this.numNodes; i++) {
            if (current.value.getKey().equals(key)) {
                return current.value;
            }
            current = current.next;
        }
        return null;
    }
}

// I chose to make everything in this class static because
// it contains multiple methods that need to be grouped
// together, but the class is stateless, so no benefit would be
// gained by having an instance of it
class MurmurHash3 {
    static int seed = 0;

    static void seed() {

        seed = randInt(0, Integer.MAX_VALUE);
    }

    static int randInt(int min, int max) {
        /**
         * Returns a random integer from min (inclusive)
         * to max (exclusive)
         */
        Random rand = new Random();
        return rand.nextInt(max - min) + min;
    }

    static int rotl(int x, int r) {
        /**
         * circularly rotates bits r spaces to the left
         */
        return (x << r) | (x >> (32 - r));
    }

    static int fmix(int h) {
        /**
         * forces all bits to avalanche
         */
        h ^= h >> 16;
        h *= 0x85ebca6b;
        h ^= h >> 13;
        h *= 0xc2b2ae35;
        h ^= h >> 16;
        return h;
    }

    static int get4ByteChunk(byte[] data, int i) {
        /**
         * Returns an integer consisting of the data from 4 bytes of the byte[]
         */
        return (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8) |
                ((data[i + 2] & 0xff) << 16) | (data[i + 3] << 24);
    }

    static int createHash(String key) {
        /**
         * Uses MurmurHash3, created by Austin Appleby, to hash the key
         *
         * The homepage for MurmurHash can be found at https://code.google.com/p/smhasher/
         *
         * I based the code for this off the C++ version found at:
         * https://code.google.com/p/smhasher/source/browse/trunk/MurmurHash3.cpp?spec=svn136&r=136
         *
         * I chose this hash function for a few reasons:
         * - Simplicity in terms of generated assembly instructions
         * - Support for seeding
         * - Good performance on avalanche tests
         * - Native support for a 32-bit implementation (a 32-bit number is needed to index
         *   into an array)
         * - Speediness compared to other well-known hash functions
         *
         * Other approaches I considered:
         * - Jenkins Hash was discarded due to performance on avalanche tests
         * - City Hash was discarded because even though it offers a 32-bit version, it is tuned
         *   for the 64-bit version
         *
         * Other sources I used:
         * - http://research.neustar.biz/tag/murmur-hash/
         * - https://github.com/google/cityhash/issues/2
         * - https://code.google.com/p/smhasher/wiki/MurmurHash3
         */
        byte[] data = key.getBytes();
        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;
        final int len = data.length;
        final int m = 5;
        final int n = 0xe6546b64;
        final int CHUNK_SIZE = 4;

        int h1 = seed;

        // Figure out how many blocks of 4-bytes we can use
        int roundedEnd = (len & 0xfffffffc);

        for (int i = 0; i < roundedEnd; i += CHUNK_SIZE) {
            int k1 = get4ByteChunk(data, i);
            k1 *= c1;
            k1 = rotl(k1, 15);
            k1 *= c2;

            h1 ^= k1;
            h1 = rotl(h1, 13);
            h1 = h1 * m + n;
        }

        int k1 = 0;

        // Hash the remaining bytes
        switch(len & 0x03) {
            case 3:
                k1 ^= (data[roundedEnd + 2] & 0xff) << 16;
            case 2:
                k1 ^= (data[roundedEnd + 1] & 0xff) << 8;
            case 1:
                k1 ^= (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = rotl(k1, 15);
                k1 *= c2;
                h1 ^= k1;
        }

        h1 ^= len;

        h1 = fmix(h1);

        return h1;
    }
}