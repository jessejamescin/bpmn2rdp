package br.upe.dsc.bpel2net;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import br.upe.dsc.calo.simplex.SimplexReader2;



public class Main {

    public static void main(String[] args) {
        
    	System.out.println("BPEL-2-Net Compiler");
    	
    	File f = new File(args[0]);
    	
    	System.out.println("Compilando...");
    	
    	new BPELCompiler().compile(f);
    	
    	System.out.println("Concluído.");

    }

}
