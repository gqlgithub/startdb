package imp;

import table.FileChannelTable;
import table.Table;
import utils.InternalTableIterator;

/**
 * @Description:
 * @Author:
 * @Date: 2026-09-07 21:16
 **/
// todo
public class TableCache {

    public Table get(long fileNumber){
        Table table = null;
        if(false){
            // mmap
        }else {
            table = new FileChannelTable();
        }

        return table;
    }

    public InternalTableIterator newIterator(FileMeta fileMeta){
        return new InternalTableIterator(get(fileMeta.getFileNumber()).iterator());
    }

}
