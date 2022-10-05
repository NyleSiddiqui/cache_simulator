public class Cache {
    int size;
    int assoc;
    int blocksize;
    int sets = size / (assoc * blocksize);
    int[][] mat = new int[sets][assoc];
}
