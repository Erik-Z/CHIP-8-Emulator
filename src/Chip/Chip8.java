package Chip;

import java.io.*;
import java.util.Random;

// Opcodes for CHIP-8: https://en.wikipedia.org/wiki/CHIP-8

public class Chip8 {
    private char[] memory;      //Size 4096
    private char[] V;           //Registers
    private char I;             //Index
    private char pc;            //Program Counter

    private char[] stack;
    private int stack_pointer;

    private int delay_timer;
    private int sound_timer;

    private byte[] keys;

    private byte[] display;

    private boolean draw_flag;

    public void init(){
        memory = new char[4096];
        V = new char[16];
        I = 0;
        pc = 0x200;

        stack = new char[16];
        stack_pointer = 0;

        delay_timer = 0;
        sound_timer = 0;

        keys = new byte[16];
        display = new byte[64 * 32];

        draw_flag = false;
        loadFontSet();
    }

    public void run(){
        //Fetch Opcode
        char optcode = (char)((memory[pc] << 8)| memory[pc + 1]);
        // Opcode has 4 digits 0x____.
        // The first digit is the function and last 3 are parameters.
        System.out.print(Integer.toHexString(optcode) + ": ");
        switch (optcode & 0xF000) {
            case 0x0000:{ // 0x00E0 or 0x00EE Not including Super Chip 8 Opcodes
                switch (optcode & 0x00FF){
                    case 0x00E0: { // 0x00E0: Clear Display
                        for (int i = 0; i < display.length; i++) {
                            display[i] = 0;
                        }
                        pc += 2;
                        draw_flag = true;
                        System.out.println("Clear Screen");
                        break;
                    }
                    case 0x00EE: // 0x00E0: Returns Function Call
                        stack_pointer--;
                        pc = (char) (stack[stack_pointer] + 2);
                        System.out.println("Return to " + Integer.toHexString(pc));
                        break;
                    default:
                        System.err.println("Super Chip 8 opcodes not supported.");
                        System.exit(0);
                }
                break;
            }
            case 0x1000: // 0x1NNN Jumps to address  NNN
                code0x1000(optcode);
                break;
            case 0x2000: // 0x2NNN: Calls Function at address  NNN
                code0x2000(optcode);
                break;
            case 0x3000: // 0x3XNN: Skips next instruction if Vx == NN
                code0x3000(optcode);
                break;
            case 0x4000: // 0x4XNN: Skips next instruction if Vx != NN
                code0x4000(optcode);
                break;
            case 0x5000: // 0x5XY0: Skips the next instruction if VX equals VY.
                code0x5000(optcode);
                break;
            case 0x6000: // 0x6XNN: Sets Vx = NN
                code0x6000(optcode);
                break;
            case 0x7000: // 0x7XRR: Adds RR to Vx
                code0x7000(optcode);
                break;
            case 0x8000: // 0x8XYN Contains data in N
                switch (optcode & 0x000F){
                    case 0x0000: {// 0x8XY0: move Vx to Vy (Vx = Vy)
                        int x = (optcode & 0x0F00) >> 8;
                        int y = (optcode & 0x00F0) >> 4;
                        V[x] = V[y];
                        pc += 2;
                        System.out.println("Set v" + x + " = " + (int) V[x]);
                        break;
                    }
                    case 0x0001: { // 0x8XY1: Sets Vx = Vx | Vy
                        int x = (optcode & 0x0F00) >> 8;
                        int y = (optcode & 0x00F0) >> 4;
                        V[x] = (char)((V[x] | V[y]) & 0xFF);
                        pc += 2;
                        System.out.println("Set v" + x + " = " + (int)V[x]);
                        break;
                    }
                    case 0x0002: { // 0x8XY2: Sets Vx = Vx & Vy
                        int x = (optcode & 0x0F00) >> 8;
                        int y = (optcode & 0x00F0) >> 4;
                        V[x] = (char) (V[x] & V[y]);
                        pc += 2;
                        System.out.println("Set v" + x + " = " + (int) V[x]);
                        break;
                    }
                    case 0x0003: { // 0x8XY3: Sets Vx = Vx XOR Vy *
                        int x = (optcode & 0x0F00) >> 8;
                        int y = (optcode & 0x00F0) >> 4;
                        V[x] = (char) ((V[x] ^ V[y]) & 0xFF);
                        pc += 2;
                        System.out.println("Set v" + x + " = " + (int) V[x]);
                        break;
                    }
                    case 0x0004: { // 0x8XY3: Sets Vx = Vx + Vy, VF = 1 if carry over
                        int x = (optcode & 0x0F00) >> 8;
                        int y = (optcode & 0x00F0) >> 4;
                        if (V[y] > 255 - V[x]){
                            V[0xF] = 1;
                        } else {
                            V[0xF] = 0;
                        }
                        V[x] = (char) ((V[x] + V[y]) & 0x00FF);
                        pc += 2;
                        System.out.println("Set v" + x + " = " + (int) V[x]);
                        break;
                    }
                    case 0x0005: { // 0x8XY3: Sets Vx = Vx - Vy, VF = 0 if borrow
                        int x = (optcode & 0x0F00) >> 8;
                        int y = (optcode & 0x00F0) >> 4;
                        if (V[x] > V[y]){
                            V[0xF] = 1;
                        } else {
                            V[0xF] = 0;
                        }
                        V[x] = (char) ((V[x] - V[y]) & 0xFF);
                        pc += 2;
                        System.out.println("Set v" + x + " = " + (int) V[x]);
                        break;
                    }
                    case 0x0006: { // 0x8XY6: Sets VF = LSB of Vx, Shift Vx to the right by 1
                        int x = (optcode & 0x0F00) >> 8;
                        int LSB = V[x] & 0x1;
                        V[0xF] = (char) LSB;
                        V[x] = (char) (V[x] >> 1);
                        pc += 2;
                        System.out.println("v16 = " + LSB + ", v" + x + " = " + (int) V[x]);
                        break;
                    }
                    case 0x0007: { // 0x8XY7 Sets VX to VY minus VX. VF = 0 if borrow, else 1.
                        int x = (optcode & 0x0F00) >> 8;
                        int y = (optcode & 0x00F0) >> 4;
                        if (V[x] > V[y]){
                            V[0xF] = 0;
                        } else {
                            V[0xF] = 1;
                        }
                        V[x] = (char) ((V[y] - V[x]) & 0xFF);
                        pc += 2;
                        System.out.println("Set v" + x + " = " + (int) V[x]);
                        break;
                    }
                    case 0x000E: { // 0x8XYE: Sets VF = MSB of Vx, Shift Vx to the left by 1
                        int x = (optcode & 0x0F00) >> 8;
                        int MSB = V[x] & 0x80;
                        V[0xF] = (char) MSB;
                        V[x] = (char) (V[x] << 1);
                        pc += 2;
                        System.out.println("v16 = " + MSB + ", v" + x + " = " + (int) V[x]);
                        break;
                    }
                    default:
                        System.err.println("Unknown Optcode");
                        System.exit(0);
                }
                break;
            case 0x9000: // 0x9XY0: Skips the next instruction if VX does not equal VY.
                code0x9000(optcode);
                break;
            case 0xA000: // 0xANNN Sets I = NNN
                code0xA000(optcode);
                break;
            case 0xB000: // 0xBNNN Jumps to the address NNN plus V0.
                code0xB000(optcode);
                break;
            case 0xC000: // 0xCXNN: Set vX to random # AND NN
                code0xC000(optcode);
                break;
            case 0xD000: {// 0xDXYN: Draws Sprite at (VX, VY) with Size (8, N). Sprite is in I.
                int x = V[(optcode & 0x0F00) >> 8];
                int y = V[(optcode & 0x00F0) >> 4];
                int N = optcode & 0x000F;
                V[0xF] = 0; // Collision Flag
                for(int _y = 0; _y < N; _y++){
                    int line = memory[I + _y];
                    for(int _x = 0; _x < 8; _x++){
                        int pixel = line & (0x80 >> _x);
                        if(pixel != 0){
                            int totalX = x + _x;
                            int totalY = y + _y;

                            totalX = totalX % 64;
                            totalY = totalY % 32;

                            int index = totalY * 64 + totalX;
                            if (display[index] == 1) {
                                V[0xF] = 1;
                            }
                            display[index] ^= 1;
                        }
                    }
                }
                pc += 2;
                draw_flag = true;
                System.out.println("Draws");
                break;
            }
            case 0xE000:
                code0xE000(optcode);
                break;
            case 0xF000:
                switch (optcode & 0x00FF){
                    case 0x0007: { // 0xFR07: Set vR = delay_timer
                        int R = (optcode & 0x0F00) >> 8;
                        V[R] = (char) delay_timer;
                        pc += 2;
                        System.out.println("v" + R + " = " + delay_timer);
                        break;
                    }
                    case 0x000A: { // 0xFR0A: Await key press. Store key press in Vx
                        int R = (optcode & 0x0F00) >> 8;
                        for(int i = 0; i < keys.length; i++){
                            if(keys[i] == 1){
                                V[R] = (char) i;
                                pc += 2;
                                break;
                            }
                        }
                        System.out.println("Waiting for key press.");
                        break;
                    }
                    case 0x0015: { // 0xFR15: Set Delay Timer to vR
                        int R = (optcode & 0x0F00) >> 8;
                        delay_timer = V[R];
                        pc += 2;
                        System.out.println("Delay Timer = " + V[R]);
                        break;
                    }
                    case 0x0018: { // 0xFR18: Set Sound Timer to vR
                        int R = (optcode & 0x0F00) >> 8;
                        sound_timer = V[R];
                        pc += 2;
                        System.out.println("Sound Timer = " + V[R]);
                        break;
                    }
                    case 0x001E: { // 0xFR1E: I += vR
                        int R = (optcode & 0x0F00) >> 8;
                        I = (char) (I + V[R]);
                        pc += 2;
                        System.out.println("I = " + V[R]);
                        break;
                    }
                    case 0x0029: { //0xFR29 Set I to location of sprite for char VR (FontSet)
                        int R = (optcode & 0x0F00) >> 8;
                        I = (char) (0x50 + (V[R] * 5));
                        pc += 2;
                        System.out.println("Setting I to " + Integer.toHexString(I));
                        break;
                    }
                    case 0x0033: { // 0xFR33: Set Binary-Coded-Decimal of Vr at location I,I+1,I+2
                        // Value of I must remain unchanged
                        int value = V[(optcode & 0x0F00) >> 8];
                        int hundred = (value - (value % 100)) / 100;
                        value = value % 100;
                        int ten = (value - (value % 10)) / 10;
                        value = value % 10;
                        int one = value;
                        memory[I] = (char) hundred;
                        memory[I + 1] = (char) ten;
                        memory[I + 2] = (char) one;
                        System.out.println("Storing (" + hundred + "," + ten + "," + one + ") at " + Integer.toHexString(I));
                        pc += 2;
                        break;
                    }
                    case 0x0055: { // 0xFR55: Stores v0 - vR into memory[I to I + R]
                        int R = (optcode & 0x0F00) >> 8;
                        for(int i = 0; i < R + 1; i++) {
                            memory[I + i] = V[i];
                        }
                        I = (char) (I + R + 1);
                        pc += 2;
                        System.out.println("Setting memory address to v0 - v" + R);
                        break;
                    }
                    case 0x0065: { // 0xFR65: Loads v0 - vR from memory[I to I + R]
                        int R = (optcode & 0x0F00) >> 8;
                        for(int i = 0; i < R + 1; i++) {
                            V[i] = memory[I + i];
                        }
                        I = (char) (I + R + 1);
                        pc += 2;
                        System.out.println("Setting V[0] to V[" + R + "] to the values of memory[0x" + Integer.toHexString(I & 0xFFFF).toUpperCase() + "]");
                        break;
                    }
                    default:
                        System.err.println("Super Chip 8 opcodes not supported.");
                        System.exit(0);
                }
                break;
            default:
                System.err.println("Unknown Opcode");
                System.exit(0);
        }
        if(sound_timer > 0){
            sound_timer -= 1;
        }
        if(delay_timer > 0){
            delay_timer -= 1;
        }
    }

    public byte[] getDisplay(){
        return display;
    }

    public boolean needsRedraw(){
        return draw_flag;
    }

    public void removeDrawFlag(){
        draw_flag = false;
    }


    /**
     * Loads the file in bytes into Chip Memory starting at 0x200
     * @param file location of the file
     */
    public void loadProgram(String file){
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(new File(file)));
            int offset = 0;
            while(input.available() > 0){
                memory[0x200 + offset] = (char) (input.readByte() & 0xFF);
                offset++;
            }
        } catch (IOException e){
            e.printStackTrace();
            System.exit(0);
        } finally {
            if(input != null){
                try { input.close(); } catch (IOException e) { e.printStackTrace(); }
            }
        }
    }

    public void loadFontSet(){
        for(int i = 0; i < ChipData.fontset.length; i++){
            memory[0x50 + i] = (char) (ChipData.fontset[i] & 0xFF);
        }
    }

    public void setKeyBuffer(int[] keyBuffer) {
        for(int i = 0; i < keys.length; i++) {
            keys[i] = (byte)keyBuffer[i];
        }
    }

    private void code0x1000(char opcode){
        int address = opcode & 0x0FFF;
        pc = (char) address;
        System.out.println("Jumps to " + Integer.toHexString(address));
    }

    private void code0x2000(char opcode){
        stack[stack_pointer] = pc;
        stack_pointer++;
        pc = (char) (opcode & 0xFFF);
        System.out.println("Calls Function At " + Integer.toHexString(pc));
    }

    private void code0x3000(char opcode){
        int X = (opcode & 0x0F00) >> 8;
        int NN = (opcode & 0x00FF);
        if(V[X] == NN) {
            pc += 4;
            System.out.println("Skipping next instruction (V[" + X +"] == " + NN + ")");
        } else {
            pc += 2;
            System.out.println("Not skipping next instruction (V[" + X +"] != " + NN + ")");
        }
    }

    private void code0x4000(char optcode) {
        int X = (optcode & 0x0F00) >> 8;
        int NN = (optcode & 0x00FF);
        if(V[X] != NN) {
            pc += 4;
            System.out.println("Skipping next instruction (V[" + X +"] != " + NN + ")");
        } else {
            pc += 2;
            System.out.println("Not skipping next instruction (V[" + X +"] == " + NN + ")");
        }
    }

    private void code0x5000(char opcode){
        int x = (opcode & 0x0F00) >> 8;
        int y = (opcode & 0x00F0) >> 4;
        if(V[x] == V[y]){
            pc += 4;
            System.out.println("Skipping next instruction: v" + x + " == v" + y);
        } else {
            pc += 2;
            System.out.println("Not skipping next instruction: v" + x + " != v" + y);
        }
    }

    private void code0x6000(char optcode){
        int register = (optcode & 0x0F00) >> 8;
        V[register] = (char) (optcode & 0x00FF);
        pc += 0x2;
        System.out.println("V" + register + " = " + Integer.toHexString((optcode & 0x00FF)));
    }

    private void code0x7000(char optcode){
        int register = (optcode & 0x0F00) >> 8;
        int RR = optcode & 0x00FF;
        V[register] = (char) ((V[register] + RR) & 0xFF);
        pc += 0x2;
        System.out.println("Adds " + RR + " to V" + register);
    }

    private void code0x9000(char opcode){
        int x = (opcode & 0x0F00) >> 8;
        int y = (opcode & 0x00F0) >> 4;
        if(V[x] != V[y]){
            pc += 4;
            System.out.println("Skipping next instruction: v" + x + " != v" + y);
        } else {
            pc += 2;
            System.out.println("Not skipping next instruction: v" + x + " == v" + y);
        }
    }

    private void code0xA000(char opcode){
        I = (char) (opcode & 0xFFF);
        System.out.println("I = " + Integer.toHexString(I));
        pc += 2;
    }

    private void code0xB000(char opcode){
        int NNN = (opcode & 0x0FFF);
        pc = (char) (NNN + (V[0] & 0xFF));
        System.out.println("Jumps to address " + Integer.toHexString((int) pc));
    }

    private void code0xC000(char optcode) {
        int x = (optcode & 0x0F00) >> 8;
        int NN = optcode & 0x00FF;
        int random = new Random().nextInt(256) & NN;
        V[x] = (char) random;
        pc += 2;
        System.out.println("Randomize: v" + x + " = " + random);
    }

    private  void code0xE000(char opcode){
        switch (opcode & 0x00FF){
            case 0x009E: { // 0xEK9E: skip if key (register vk) pressed
                int key = V[(opcode & 0x0F00) >> 8];
                if(keys[key] == 1) {
                    pc += 4;
                } else {
                    pc += 2;
                }
                System.out.println("Skipping next instruction if V[" + key + "] is pressed");
                break;
            }
            case 0x00A1: { // 0xEKA1: skip if key (register vk) not pressed
                int key = V[(opcode & 0x0F00) >> 8];
                if(keys[key] == 0) {
                    pc += 4;
                } else {
                    pc += 2;
                }
                System.out.println("Skipping next instruction if V[" + key + "] is NOT pressed");
                break;
            }
            default:
                System.err.println("Unknown opcode");
                System.exit(0);
        }
    }
}
