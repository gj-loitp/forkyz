package app.crossword.yourealwaysbe.io.versions;

import app.crossword.yourealwaysbe.io.IO;
import app.crossword.yourealwaysbe.puz.Puzzle;
import app.crossword.yourealwaysbe.puz.PuzzleMeta;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class IOVersion2 extends IOVersion1 {

    @Override
    protected void applyMeta(Puzzle puz, PuzzleMeta meta){
        super.applyMeta(puz, meta);
        puz.setSourceUrl(meta.sourceUrl);
    }

    @Override
    public PuzzleMeta readMeta(DataInputStream dis) throws IOException{
        PuzzleMeta meta = super.readMeta(dis);
        dis.read(); // read old updatable byte
        meta.sourceUrl = IO.readNullTerminatedString(dis, getCharset());
        return meta;
    }

    @Override
    protected void writeMeta(Puzzle puz, DataOutputStream dos)
              throws IOException {
        super.writeMeta(puz, dos);
        dos.write(1); // old updatable byte
        IO.writeNullTerminatedString(dos, puz.getSourceUrl(), getCharset());
    }
}
