package tmp;

/**
 * Created by mercenery on 18.06.2017.
 */
public class whileWhile{
	
	public static void main(String[] args){
		int i  = 10;
		int j = 5;
		while(i>0){
			while(j>0){
				System.out.println(j);
				j--;
				if(j==2){
					return;
				}
			}
			System.out.println(i);
			i--;
		}
	}
}
