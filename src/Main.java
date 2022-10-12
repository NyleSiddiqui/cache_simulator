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
    static int global_counter = 1;

    static int blocksize;
    static int l1size;
    static int l1assoc;
    static int l2size;
    static int l2assoc ;
    static int l2sets;
    static int l1sets;
    static Block[][] l1cache;
    static Block[][] l2cache;

    public static Block make_block(String address_in, int cache_level){
        // index bits = log2(32) = 5
        // block offset bits - log2(16) = 4
        // tag =  32 - 9 = 23
        String full_address = new BigInteger(address_in, 16).toString(2); // Convert hex address to binary string
        while(full_address.length() != 32){
            full_address = "0" + full_address;
        }
        int set_index_bits;
        if(cache_level == 1){
            set_index_bits = (int)(Math.log(l1sets) / Math.log(2));
        } else {
            set_index_bits = (int)(Math.log(l2sets) / Math.log(2));
        }
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
        new_block.entry_time = new_block.LRU = global_counter;
        return new_block;
    }

    public static void l1_read(Block new_block){
        for(int i=0; i < l1assoc; i++){ // loop through each element in the new block's set to find block to be read
            if(l1cache[new_block.set_index][i].valid) {
                if (l1cache[new_block.set_index][i].tag.equals(new_block.tag)) {
                    l1cache[new_block.set_index][i].LRU = global_counter;
                    l1cache[new_block.set_index][i].dirty = false;
                    return;
                }
            }
        }
        l1_read_misses++;
        l1_write(new_block, true);
        //l2_read(new_block);
    }

    public static void replacement(Block block, boolean read){
        switch (replacement_policy) {
            case "lru" -> {
                Block lru = l1cache[block.set_index][0];
                int assoc_index = 0;
                for (int i = 1; i < l1assoc; i++) {
                    if ((global_counter - l1cache[block.set_index][i].LRU) > (global_counter - lru.LRU)) {
                        lru = l1cache[block.set_index][i];
                        assoc_index = i;
                    }
                }
                if (lru.dirty) {
                    //l2_write(lru);
                }
                l1cache[lru.set_index][assoc_index] = block;
                l1cache[block.set_index][assoc_index].valid = true;
                if (!read) {
                    l1cache[block.set_index][assoc_index].dirty = true;
                    l1_write_misses++;
                }
            }
            case "fifo" -> {
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
                return;
            }
            case "optimal" -> System.out.println("pwacehowder");
        }
    }

    public static void l1_write(Block block, boolean read){
            for (int i = 0; i < l1assoc; i++) {
                if (!l1cache[block.set_index][i].valid) {
                    l1cache[block.set_index][i] = block;
                    l1cache[block.set_index][i].valid = true;
                    l1cache[block.set_index][i].LRU = global_counter;
                    if(!read) {
                        l1cache[block.set_index][i].dirty = true;
                        l1_write_misses++;
                    }
                    return;
                } else if (l1cache[block.set_index][i].tag.equals(block.tag)) {
                    l1cache[block.set_index][i].dirty = true;
                    l1cache[block.set_index][i].LRU = global_counter;
                    return;
                }
            }
            // all blocks in the set are valid and not equal to new block tag
            if (l2size == 0){
                replacement(block, read);
            } else {
                System.out.println("pwacehowder");
                //TODO: l2_write();
            }

    }



    public static void main(String args[]) throws IOException {
        blocksize = Integer.parseInt(args[0]);
        l1size = Integer.parseInt(args[1]);
        l1assoc = Integer.parseInt(args[2]);
        l2size = Integer.parseInt(args[3]);
        l2assoc = Integer.parseInt(args[4]);
        replacement_policy = args[5];
        inclusion_poliocy = args[6];
        String trace_file = args[7];
        l1sets = l1size / (l1assoc * blocksize);
        if (l2size != 0) {
            l2sets = l2size / (l2assoc * blocksize);
        }

        l1cache = new Block[l1sets][l1assoc];
        l2cache = new Block[l2sets][l2assoc];

        for(int i=0; i < l1sets; i++){
            for(int j=0; j < l1assoc; j++){
                l1cache[i][j] = new Block();
            }
        }

        String path = "C://Users/ny525072/IdeaProjects/cache_simulator/MachineProblem1/traces/gcc_trace.txt";
        //String path = "C:/School/School PhDizzle/CDA 5106/MachineProblem1/traces/gcc_trace.txt";
        BufferedReader console = new BufferedReader(new FileReader(path));
        String line = console.readLine();
        String instruction;
        String address;
        while (line != null){
            String[] trace = line.split("\\s+");
            instruction = trace[0];
            address = trace[1];
            Block block = make_block(address, 1);

            if (instruction.equals("r")){
                l1_reads++;
                l1_read(block);
            }
            else if (instruction.equals("w")){
                l1_writes++;
                l1_write(block, false);
            }
            line = console.readLine();
            global_counter++;

        }
        console.close();
        System.out.println("===== Simulator configuration =====");
        System.out.println("BLOCKSIZE: " + blocksize);
        System.out.println("L1_SIZE: " + l1size);
        System.out.println("L1_ASSOC: " + l1assoc);
        System.out.println("L2_SIZE: " + l2size);
        System.out.println("L2_ASSOC: " + l2assoc);
        System.out.println("REPLACEMENT POLICY: " + replacement_policy);
        System.out.println("INCLUSION PROPERTY: " + inclusion_poliocy);
        System.out.println("trace_file: " + trace_file);
        System.out.println("===== L1 contents =====");
        for(int i=0; i<l1sets; i++) {
            System.out.print("SET " + i + "    ");
            for (int j = 0; j < l1assoc; j++) {
                String hex_add = new BigInteger(l1cache[i][j].tag, 2).toString(16);
                while(hex_add.length() != 6){
                    hex_add = "0" + hex_add;
                }
                System.out.print(hex_add + "   ");
                if(l1cache[i][j].dirty){
                    System.out.print(" D ");
                }
            }
            System.out.println();
        }
        System.out.println("===== Simulation results (raw) =====");
        System.out.println("a. number of L1 reads: " + l1_reads);
        System.out.println("b. number of L1 read misses: " + l1_read_misses);
        System.out.println("c .number of L1 writes: " + l1_writes);
        System.out.println("d. number of L1 write misses: " + l1_write_misses);
        System.out.println(global_counter);
    }
}