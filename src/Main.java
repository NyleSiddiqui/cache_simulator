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
    static int l1_writebacks = 0;
    static int l2_writebacks = 0;
    static String replacement_policy;
    static String inclusion_policy;
    static int global_counter = 1;
    static int direct_mem_write = 0;

    static int blocksize;
    static int l1size;
    static int l1assoc;
    static int l2size;
    static int l2assoc ;
    static int l2sets;
    static int l1sets;
    static Block[][] l1cache;
    static Block[][] l2cache;

    public static Block make_block(String address_in, int cache_level, int optimal){
        // index bits = log2(32) = 5
        // block offset bits - log2(16) = 4
        // tag =  32 - 9 = 23
        String full_address = address_in;
        if (cache_level == 1) {
            full_address = new BigInteger(address_in, 16).toString(2); // Convert hex address to binary string
        }
        while(full_address.length() != 32){
            full_address = "0" + full_address;
        }
        int set_index_bits;
        if(cache_level == 1 || cache_level == 3){
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
        new_block.full_address = full_address;
        if (replacement_policy.equals("optimal")){
            new_block.optimal = optimal;
        }
        return new_block;
    }


    public static void invalidate(Block block){
        for(int i =0; i<l1assoc; i++){
            if (l1cache[block.set_index][i].tag.equals(block.tag)){
                if(l1cache[block.set_index][i].dirty = true){
                    direct_mem_write++;
                }
                l1cache[block.set_index][i].valid = false;
            }
        }
    }

    public static void replacement(Block block, boolean read, int level){
        switch (replacement_policy) {
            case "lru":
                if (level == 1) {
                    Block lru = l1cache[block.set_index][0];
                    int assoc_index = 0;
                    for (int i = 1; i < l1assoc; i++) {
                        if ((global_counter - l1cache[block.set_index][i].LRU) > (global_counter - lru.LRU)) {
                            lru = l1cache[block.set_index][i];
                            assoc_index = i;
                        }
                    }
                    if (lru.dirty) {
                        l1_writebacks++;
                        if (l2size != 0) {
                            Block temp = make_block(lru.full_address, 2, -1);
                            l2_write(temp, false);
                        }
                    }
                    l1cache[lru.set_index][assoc_index] = block;
                    l1cache[lru.set_index][assoc_index].valid = true;
                    if (!read) {
                        l1cache[lru.set_index][assoc_index].dirty = true;
                        l1_write_misses++;
                    }
                    return;
                } else {
                    Block lru = l2cache[block.set_index][0];
                    int assoc_index = 0;
                    for (int i = 1; i < l2assoc; i++) {
                        if ((global_counter - l2cache[block.set_index][i].LRU) > (global_counter - lru.LRU)) {
                            lru = l2cache[block.set_index][i];
                            assoc_index = i;
                        }
                    }
                    if (lru.dirty) {
                        l2_writebacks++;
                    }
                    if(inclusion_policy.equals("inclusive")){
                        Block tbinvalidated = make_block(lru.full_address, 3, lru.optimal);
                        invalidate(tbinvalidated);
                    }
                    l2cache[lru.set_index][assoc_index] = block;
                    l2cache[lru.set_index][assoc_index].valid = true;
                    if(!read) {
                        l2cache[lru.set_index][assoc_index].dirty = true;
                        l2_write_misses++;
                    }
                    return;
                }

            case "fifo":
                if (level == 1) {
                    int minimum = l1cache[block.set_index][0].entry_time;
                    Block victim = l1cache[block.set_index][0];
                    int assoc_index = 0;
                    for (int i = 1; i < l1assoc; i++) {
                        if (l1cache[block.set_index][i].entry_time < minimum) {
                            victim = l1cache[block.set_index][i];
                            assoc_index = i;
                        }
                    }
                    if (victim.dirty) {
                        l1_writebacks++;
                        if (l2size != 0) {
                            Block temp = make_block(victim.full_address, 2, -1);
                            l2_write(temp, false);
                        }
                    }
                    l1cache[victim.set_index][assoc_index] = block;
                    l1cache[victim.set_index][assoc_index].valid = true;
                    if (!read) {
                        l1cache[victim.set_index][assoc_index].dirty = true;
                        l1_write_misses++;
                    }
                    return;

                } else{
                    int minimum = l2cache[block.set_index][0].entry_time;
                    Block victim = l2cache[block.set_index][0];
                    int assoc_index = 0;
                    for (int i = 1; i < l2assoc; i++) {
                        if (l2cache[block.set_index][i].entry_time < minimum) {
                            victim = l2cache[block.set_index][i];
                            assoc_index = i;
                        }
                    }
                    if (victim.dirty) {
                        l2_writebacks++;
                    }
                    l2cache[victim.set_index][assoc_index] = block;
                    l2cache[victim.set_index][assoc_index].valid = true;
                    if (!read) {
                        l2cache[victim.set_index][assoc_index].dirty = true;
                        l2_write_misses++;
                    }
                    return;
                }

            case "optimal" :
                if (level == 1) {
                    Block opt = l1cache[block.set_index][0];
                    int assoc_index = 0;
                    for (int i = 1; i < l1assoc; i++) {
                        if ((l1cache[block.set_index][i].optimal - global_counter) > (opt.optimal - global_counter)) {
                            opt = l1cache[block.set_index][i];
                            assoc_index = i;
                        }
                    }
                    if (opt.dirty) {
                        l1_writebacks++;
                        if (l2size != 0) {
                            Block temp = make_block(opt.full_address, 2, opt.optimal);
                            l2_write(temp, false);
                        }
                    }
                    l1cache[opt.set_index][assoc_index] = block;
                    l1cache[opt.set_index][assoc_index].valid = true;
                    if (!read) {
                        l1cache[opt.set_index][assoc_index].dirty = true;
                        l1_write_misses++;
                    }
                    break;

                } else {
                    Block opt = l2cache[block.set_index][0];
                    int assoc_index = 0;
                    for (int i = 1; i < l2assoc; i++) {
                        if ((l2cache[block.set_index][i].optimal - global_counter) > (opt.optimal - global_counter)) {
                            opt = l2cache[block.set_index][i];
                            assoc_index = i;
                        }
                    }
                    if (opt.dirty) {
                        l2_writebacks++;
                        if (l2size != 0) {
                            Block temp = make_block(opt.full_address, 2, opt.optimal);
                            l2_write(temp, false);
                        }
                    }
                    l2cache[opt.set_index][assoc_index] = block;
                    l2cache[opt.set_index][assoc_index].valid = true;
                    if (!read) {
                        l2cache[opt.set_index][assoc_index].dirty = true;
                        l2_write_misses++;
                    }
                    break;
                }
        }
    }

    public static void l1_read(Block new_block){
        for(int i=0; i < l1assoc; i++){ // loop through each element in the new block's set to find block to be read
            if(l1cache[new_block.set_index][i].valid) {
                if (l1cache[new_block.set_index][i].tag.equals(new_block.tag)) {
                    l1cache[new_block.set_index][i].LRU = global_counter;
                    return;
                }
            }
        }
        l1_read_misses++;
        l1_write(new_block, true);
        if (l2size != 0) {
            new_block = make_block(new_block.full_address, 2, -1);
            l2_read(new_block);
        }
    }


    public static void l2_read(Block block){
        l2_reads++;
        for(int i=0; i < l2assoc; i++){ // loop through each element in the new block's set to find block to be read
            if(l2cache[block.set_index][i].valid) {
                if (l2cache[block.set_index][i].tag.equals(block.tag)) {
                    l2cache[block.set_index][i].LRU = global_counter;
                    return;
                }
            }
        }
        l2_read_misses++;
        l2_write(block, true);
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
                    if (l2size != 0) {
                        block = make_block(block.full_address, 2, -1);
                        l2_read(block);
                    }
                }
                return;
            } else if (l1cache[block.set_index][i].tag.equals(block.tag)) {
                l1cache[block.set_index][i].dirty = true;
                l1cache[block.set_index][i].LRU = global_counter;
                return;
            }
        }
        // all blocks in the set are valid and not equal to new block tag
        replacement(block, read, 1);
        if (l2size != 0 && !read) {
            block = make_block(block.full_address, 2, -1);
            l2_read(block);
        }
    }

    public static void l2_write(Block block, boolean read){
        if (!read) {
            l2_writes++;
        }
        for (int i = 0; i < l2assoc; i++) {
            if (!l2cache[block.set_index][i].valid) {
                l2cache[block.set_index][i] = block;
                l2cache[block.set_index][i].valid = true;
                l2cache[block.set_index][i].LRU = global_counter;
                if(!read) {
                    l2cache[block.set_index][i].dirty = true;
                    l2_write_misses++;
                    block = make_block(block.full_address, 2, -1);
                    l2_read(block);
                }
                return;
            } else if (l2cache[block.set_index][i].tag.equals(block.tag)) {
                l2cache[block.set_index][i].dirty = true;
                l2cache[block.set_index][i].LRU = global_counter;
                return;
            }
        }
        // all blocks in the set are valid and not equal to new block tag
        replacement(block, read, 2);
    }


    public static void main(String args[]) throws IOException {
        blocksize = Integer.parseInt(args[0]);
        l1size = Integer.parseInt(args[1]);
        l1assoc = Integer.parseInt(args[2]);
        l2size = Integer.parseInt(args[3]);
        l2assoc = Integer.parseInt(args[4]);
        int policy = Integer.parseInt(args[5]);
        switch(policy){
            case 0:
                replacement_policy = "lru";
                break;
            case 1:
                replacement_policy = "fifo";
                break;
            case 2:
                replacement_policy = "optimal";
                break;
        }
        int ipolicy = Integer.parseInt(args[6]);
        switch(ipolicy) {
            case 0:
                inclusion_policy = "non-inclusive";
                break;
            case 1:
                inclusion_policy = "inclusive";
                break;
        }
        String trace_file = args[7];

        l1sets = l1size / (l1assoc * blocksize);
        l1cache = new Block[l1sets][l1assoc];
        for(int i=0; i < l1sets; i++){
            for(int j=0; j < l1assoc; j++){
                l1cache[i][j] = new Block();
            }
        }

        if (l2size != 0) {
            l2sets = l2size / (l2assoc * blocksize);
            l2cache = new Block[l2sets][l2assoc];
            for(int i=0; i < l2sets; i++){
                for(int j=0; j < l2assoc; j++){
                    l2cache[i][j] = new Block();
                }
            }
        }

        //String path = "C://Users/ny525072/IdeaProjects/cache_simulator/MachineProblem1/traces/" + trace_file;
        String path = "C:/School/CS 355/MachineProblem1/traces/vortex_trace.txt";
        BufferedReader console = new BufferedReader(new FileReader(path));
        String line = console.readLine();
        Block block;
        int count;
        String instruction;
        String address;
        while (line != null){
            String[] trace = line.split("\\s+");
            instruction = trace[0];
            address = trace[1];
            if (replacement_policy.equals("optimal")) {
                int optimal = 0;
                String nextLine;
                count = 1;
                BufferedReader distance = new BufferedReader(new FileReader(path));
                while (count < global_counter){
                    count ++;
                    nextLine = distance.readLine();
                }
                nextLine = distance.readLine();
                String addy = nextLine.split("\\s+")[1];
                count++;
                while(addy != null && !addy.equals(address)){
                    nextLine = distance.readLine();
                    addy = nextLine.split("\\s+")[1];
                    count++;
                }
                optimal = count;
                block = make_block(address, 1, optimal);
            } else {
                block = make_block(address, 1, -1);
            }

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
            System.out.println(global_counter);
        }
        console.close();
        System.out.println("===== Simulator configuration =====");
        System.out.println("BLOCKSIZE: " + blocksize);
        System.out.println("L1_SIZE: " + l1size);
        System.out.println("L1_ASSOC: " + l1assoc);
        System.out.println("L2_SIZE: " + l2size);
        System.out.println("L2_ASSOC: " + l2assoc);
        System.out.println("REPLACEMENT POLICY: " + replacement_policy);
        System.out.println("INCLUSION PROPERTY: " + inclusion_policy);
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
        if(l2size != 0) {
            System.out.println("===== L2 contents =====");
            for (int k = 0; k < l2sets; k++) {
                System.out.print("SET " + k + "    ");
                for (int j = 0; j < l2assoc; j++) {
                    String hex_add = new BigInteger(l2cache[k][j].tag, 2).toString(16);
                    while (hex_add.length() != 5) {
                        hex_add = "0" + hex_add;
                    }
                    System.out.print(hex_add + "   ");
                    if (l2cache[k][j].dirty) {
                        System.out.print(" D ");
                    }
                }
                System.out.println();
            }
        }
        System.out.println("===== Simulation results (raw) =====");
        System.out.println("a. number of L1 reads: " + l1_reads);
        System.out.println("b. number of L1 read misses: " + l1_read_misses);
        System.out.println("c. number of L1 writes: " + l1_writes);
        System.out.println("d. number of L1 write misses: " + l1_write_misses);
        System.out.print("e. L1 miss rate: ");
        System.out.printf("%.6f", (float)(l1_read_misses + l1_write_misses) / (l1_reads + l1_writes));
        System.out.println();
        System.out.println("f. number of L1 writebacks: " + l1_writebacks);
        System.out.println("g. number of L2 reads: " + l2_reads);
        System.out.println("h. number of L2 read misses: " + l2_read_misses);
        System.out.println("i. number of L2 writes: " + l2_writes);
        System.out.println("j. number of L2 write misses: " + l2_write_misses);
        System.out.print("k. L2 miss rate: ");
        if (l2size != 0) {
            System.out.printf("%.6f", (float) l2_read_misses / l2_reads);
        }
        else {
            System.out.print(0);
        }
        System.out.println();
        System.out.println("l. number of L2 writebacks: " + l2_writebacks);
        if (l2size == 0) {
            System.out.println("m. total memory traffic: " + (l1_read_misses + l1_write_misses + l1_writebacks));
        }
        else {
            System.out.println("m. total memory traffic: " + (l2_read_misses + l2_write_misses + l2_writebacks));
        }
    }
}