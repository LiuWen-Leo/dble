/**
 * 
 */
package io.mycat.plan.common.item.function.mathsfunc;

import java.math.BigInteger;
import java.util.List;
import java.util.zip.CRC32;

import io.mycat.plan.common.item.Item;
import io.mycat.plan.common.item.function.ItemFunc;
import io.mycat.plan.common.item.function.primary.ItemIntFunc;

public class ItemFuncCrc32 extends ItemIntFunc {

	/**
	 * @param name
	 * @param a
	 */
	public ItemFuncCrc32(Item a) {
		super(a);
	}

	@Override
	public final String funcName(){
		return "crc32";
	}

	@Override
	public void fixLengthAndDec() {
		maxLength = 10;
	}

	@Override
	public BigInteger valInt() {
		String res = args.get(0).valStr();
		if (res == null) {
			nullValue = true;
			return BigInteger.ZERO;
		}
		nullValue = false;
		CRC32 crc = new CRC32();
		crc.update(res.getBytes());
		return BigInteger.valueOf(crc.getValue());
	}

	@Override
	public ItemFunc nativeConstruct(List<Item> realArgs) {
		return new ItemFuncCrc32(realArgs.get(0));
	}
}