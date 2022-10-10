import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.io.FileReader;



public class Main{
    static int l1_reads = 0;
    static int l1_read_misses = 0;
    static int l1_writes = 0;
    static int l1_write_misses = 0;
    static int l2_reads = 0;
    static int l2_read_misses = 0;
    static int l2_writes = 0;
    static int l2_write_misses = 0;
    static String replacement_policy;
    static String inclusion_poliocy;
    static int entry_time = 0;

    static int blocksize = 32;
    static int l1size = 256;
    static int l1assoc = 1;
    static int l2size = 8192;
    static int l2assoc = 4;
    static int l2sets = l2size / (l2assoc * blocksize);
    static int l1sets = l1size / (l1assoc * blocksize);
    static int LRU_serial = 1;
    static Block[][] l1cache = new Block[l1sets][l1assoc];
    static Block[][] l2cache = new Block[l2sets][l2assoc];

    public static Block make_block(String address_in, int cache_level){
        // index bits = log2(32) = 5
        // block offset bits - log2(16) = 4
        // tag =  32 - 9 = 23
        String full_address = new BigInteger(address_in, 16).toString(2); // Convert hex address to binary string TODO: validate extra bit to make address 32 bits
        if(full_address.length() == 31){
            full_address = "0" + full_address;
        }
        if(cache_level == 1){
            int set_index_bits = (int)(Math.log(l1sets) / Math.log(2));
            int block_in_set_bits = (int)(Math.log(blocksize) / Math.log(2));
            int tag_bits = 32 - (set_index_bits + block_in_set_bits); // address will always be 32 bits
            String tag = full_address.substring(0, tag_bits); // Extract first *tag_bits* bits from binary address string
            String index_and_offset = full_address.substring(tag_bits); // Extract rest of bits
            String binary_index = index_and_offset.substring(0, set_index_bits); // Set index to first *index_bits* bits
            int decimal_index = Integer.parseInt(binary_index, 2);
            String block = index_and_offset.substring(set_index_bits); // Rest of bits are block offset
            Block new_block = new Block();
            new_block.tag = tag;
            new_block.block_index = block;
            new_block.set_index = decimal_index;
            new_block.entry_time = entry_time;
            return new_block;

        } else {
            int set_index_bits = (int)(Math.log(l2sets) / Math.log(2));
            int block_offset = (int)(Math.log(blocksize) / Math.log(2));
            int tag_bits = 32 - (set_index_bits + block_offset); // address will always be 32 bits
            String tag = full_address.substring(0, tag_bits); // Extract first *tag_bits* bits from binary address string
            String index_and_offset = full_address.substring(tag_bits); // Extract rest of bits
            String binary_index = index_and_offset.substring(0, set_index_bits); // Set index to first *index_bits* bits
            int decimal_index = Integer.parseInt(binary_index, 2);
            String block = index_and_offset.substring(set_index_bits); // Rest of bits are block offset
            Block new_block = new Block();
            new_block.tag = tag;
            new_block.block_index = block;
            new_block.set_index = decimal_index;
            new_block.entry_time = entry_time;
            new_block.LRU = 0;
            return new_block;
        }
    }
    public void l1_read(Block new_block){
        for(int i=0; i < l1assoc; i++){ // loop through each element in the new block's set to find block to be read
            if(l1cache[new_block.set_index][i].tag.equals(new_block.tag) && l1cache[new_block.set_index][i].valid){
                l1_reads++;
                return;
            }
        }
        l1_read_misses++;
        l1_write(new_block, true);
        //l2_read(new_block);
    }

    public void l1_write(Block block, boolean allocate){
        if (!allocate){
            for(int i=0; i< l1assoc; i++){
                if(l1cache[block.set_index][i].tag.equals(block.tag) && !l1cache[block.set_index][i].valid){
                    l1cache[block.set_index][i].valid = true;
                    l1cache[block.set_index][i] = block;
                    l1_writes++;
                    return;
                }
            }
            l1_write_misses++;
            //l2_write();
        } else {
            if(l2size == 0){
                for (int i=0; i < l1assoc; i++){
                    if(!l1cache[block.set_index][i].valid){
                        l1cache[block.set_index][i].valid = true;
                        l1cache[block.set_index][i] = block;
                        l1_writes++;
                    }
                }
                switch (replacement_policy){
                    case "lru":
                        System.out.println("pwacehowder");
                    case "fifo":
                        int minimum = l1cache[block.set_index][0].entry_time;
                        Block victim = l1cache[block.set_index][0];
                        for (int i=1; i < l1assoc; i++){
                            if(l1cache[block.set_index][i].entry_time < minimum){
                                victim = l1cache[block.set_index][i];
                            }
                        }
                        if(victim.dirty){
                            //TODO: kill victim
                        }
                    case "optimal":
                        System.out.println("pwacehowder");
                }
            } else {
                switch (replacement_policy) {
                    case "lru":
                        System.out.println("pwacehowder");
                    case "fifo":
                        int minimum = l1cache[block.set_index][0].entry_time;
                        Block victim = l1cache[block.set_index][0];
                        for (int i = 1; i < l1assoc; i++) {
                            if (l1cache[block.set_index][i].entry_time < minimum) {
                                victim = l1cache[block.set_index][i];
                            }
                        }
                        if (victim.dirty) {
                            //l2_write(victim);
                        }
                        //l2_read();
                    case "optimal":
                        System.out.println("pwacehowder");
                }
            }
        }

    }


    public static void main(String args[]) throws IOException {
        BufferedReader console = new BufferedReader(new FileReader("C://Users/ny525072/IdeaProjects/cache_simulator/src/validation0.txt"));
        String line = console.readLine();
        int count = 0;
        ArrayList<Integer> cache_info = new ArrayList<>();
        ArrayList<String> policies = new ArrayList<>();
        while (line != null){
            if (1 <= count && count <= 5) {
                String[] str = line.split("\\s+");
                cache_info.add((Integer.parseInt(str[1])));
            }
            else if (6 <= count && count <= 7) {
                String[] str = line.split("\\s+");
                policies.add(str[2]);
            }
            while (!line.equals("===== Simulation results (raw) =====")){
                String[] str = line.split("\\s+");
            }
            // read next line
            line = console.readLine();
            count ++;
        }
        console.close();

        blocksize = cache_info.get(0);
        l1size = cache_info.get(1);
        l1assoc = cache_info.get(2);
        l2size = cache_info.get(3);
        l2assoc = cache_info.get(4);
        replacement_policy = policies.get(1);
        inclusion_poliocy = policies.get(2);
        String[][] l1set_contents = new String[l1sets][l1assoc];
        String[][] l2set_contents = new String[l2sets][l2assoc];
        for(int i = 0; i < l1sets; i++){
            for(int j = 0; j < l1assoc; j++){
                Block block = make_block("FF0040E0", 1);
                l1cache[i][j] = block;
            }
        }


        Block block = make_block("FF0040E0", 1);
//
//        Block[][] l1cache = new Block[l1sets][l1assoc];
//        for(int i=0; i < l1sets; i++){
//            for(int j=0; j < l1assoc; j++){
//                l1cache[i][j] = new Block();
//            }
//        }
//        System.out.println(l1cache);



    }
}