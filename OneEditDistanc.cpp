class Solution {
public:
    bool isOneEditDistance(string s, string t) {
        if (s.size() < t.size()){
            return isOneEditDistance(t, s);
        }
        
        if (s.size() - t.size() > 1){
            return false;
        }
        
        
        int numOfDiff = 0;
        int j = 0;
        for(int i = 0; i < t.size(); i++){
            if (t[i] != s[j]){
                if (numOfDiff > 0){
                    return false;
                }
                
                numOfDiff++;                
                if (s.size() != t.size()){
                    i--;
                }
            }
                            
            j++;
        }
        
        if (s.size() - t.size() == 1){
            return true;
        }else{
            return numOfDiff == 1;
        }
    }
};
