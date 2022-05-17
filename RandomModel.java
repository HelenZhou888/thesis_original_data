package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import objectives.EdgeServer;
import objectives.User;
import tool.RandomNodeGenerator;

public class RandomModel {
	
	private double mAllUsers;
	private double mCost;
	private double mbenefitefficiency;
	private double mAllBenefits;
	private double mAllBenefit_square;

	private int mServersNumber;
	private int mBudget;
	private double mFairness_index;
	private int[][] mAdjacencyMatrix;
	private int[][] mUserCovered; 
	private int[][] mUserBenefits;
	private List<User> mUsers;// All Users


	
	//2021
	//private double mfairnessindex;
	private double mfairnessdegree; //make the original value = 0
	private double mfairness_efficiency;
    private double mfairness_degree;
	//no need to add another variable for caching cost, we can use the 
	
	private List<Integer> mValidUserList; //Covered Users by selected servers
	
	private List<Integer> mSelectedServerList;
	private List<Integer> mBenefitList;
	//private List<Integer> mUserList;
	private int[][] mDistanceMatrix;
	List<EdgeServer> mServers;
	private int[] user_distribution_result = new int[4];
	
	public RandomModel(int serversNumber, double fairness_index, int[][] adjacencyMatrix, int[][]distanceMatrics, int[][] userCovered, int[][] userBenefits, List<User> users, List<EdgeServer> servers) {
		mServersNumber = serversNumber;
		mFairness_index = fairness_index;
		mAdjacencyMatrix = adjacencyMatrix;
		mUserCovered = userCovered;
		mUsers = users;
		mDistanceMatrix = distanceMatrics;
		mServers = servers;
		
		
		mUserBenefits = userBenefits;

		mValidUserList = new ArrayList<>();
		mSelectedServerList = new ArrayList<>();
		//mUserList = new ArrayList<>();
		mBenefitList = new ArrayList<>();
		
	}
	
	private boolean canServerAccessSelectedServers(int server) {
		for (int ss : mSelectedServerList) {
			if (ss == server) return true;
			//if (mAdjacencyMatrix[ss][server] == 1 || mAdjacencyMatrix[server][ss] == 1) return true;
		}
		
		return false;
	}
	
	public void runRandom() {
		// randomly select first server
		RandomNodeGenerator randomGenerator = new RandomNodeGenerator();
		int randomServer;
		//randomServer = randomGenerator.getRandomNode(mServersNumber);
		
		//while (mValidUserList.size() < mUsers.size()) 
		//2021 while the fairness degree < fairness index
		//�������ó�ʼ��fairness degree���� Ȼ��������fairness index�Ƚ�
		double fairness_degree = 0;
		
		//�����fairness��ʼ��ֵΪ�� ��privateΪ����������..Ϊʲôѭ���߲�������
		mfairnessdegree = 0;
		//��ʼѭ��������fairness degree��ֵ
		//while (mfairnessdegree <= mFairness_index && mValidUserList.size() < mUsers.size()) {
		while (mValidUserList.size() < mUsers.size()) {
			randomServer = randomGenerator.getRandomNode(mServersNumber);
			//System.out.println(randomServer);
			if (canServerAccessSelectedServers(randomServer)) continue;
			
			//this is the number of caching cost
			mSelectedServerList.add(randomServer);
			
			//�����ǰ�ֱ�ӱ�randomѡ���server cover���û���ӵ�valid user list����
			for (User user : mUsers) {
				if (user.nearEdgeServers.contains(randomServer) && !mValidUserList.contains(user.id)) {
					mValidUserList.add(user.id);
				}
			}
	       //������ж��������ѭ�� without budget
		   //�������ѭ����Ҫ��ѡ���random server���ڽ�server cover��userҲ�ŵ�valid user list����
		   //����ΪʲôҪ��ѭ���� Ϊʲô��m_servernumber
			for (int i = 0; i < mServersNumber; i++) {
				if (mAdjacencyMatrix[i][randomServer] == 1 || mAdjacencyMatrix[randomServer][i] == 1) {
					// mConnectionsMap.remove(i);
					for (User user : mUsers) {
						if (user.nearEdgeServers.contains(i) && !mValidUserList.contains(user.id)) {
							mValidUserList.add(user.id);
						}
					}
				}
			}
			
		
		}
		
		//2022_calculate mobile user covered results after selected server set has been decided
		calculate_user_covered_results_onetwothree_hops();
		//�������һ��ȫ����cost��ȫ����benefit��ȫ��cover�û�������
		mCost = mSelectedServerList.size();
		mAllBenefits = calculateBenefits();//ȫ���û�benefit֮��
		mAllUsers = mValidUserList.size(); //����fairness degree�ķ�ĸ
		mAllBenefit_square = calculate_single_Benefits_square(); //fairness degree�ķ�ĸ
		
		//���ڼ���fairness degree    ����fairness_degree�� m fairness degreeһ����...
		//fairness_degree = (mAllBenefits * mAllBenefits) / (mUsers.size() * mAllBenefit_square);		
		mfairnessdegree = (mAllBenefits * mAllBenefits) / (mUsers.size() * mAllBenefit_square);	
		
		//����fairness efficiency
		//mfairness_efficiency = fairness_degree / mCost;		
		mfairness_efficiency = mfairnessdegree / mCost;
		//mAllUsers = mValidUserList.size();
		
/*		//ѭ�������Ѿ�������ˣ����ﻹ��Ҫ���������
		//���ڼ���fairness degree
		mfairness_degree = fairness_degree;		
		
		//����fairness efficiency
		mfairness_efficiency = fairness_degree / mCost;		
		
		mAllUsers = mValidUserList.size();
*/
		
	
	}
	
	/*		while (mfairnessdegree <= mfairnessindex) {
				int m = mServersNumber - 1;
				int j = m +(int)(Math.random()*(0+1-m));
				//System.out.println(j);
				if (mAdjacencyMatrix[j][randomServer] == 1 || mAdjacencyMatrix[randomServer][j] == 1) {
					// mConnectionsMap.remove(i);
					for (User user : mUsers) {
						if (user.nearEdgeServers.contains(j) && !mValidUserList.contains(user.id)) {
							mValidUserList.add(user.id);
						}
					}
				}
			}
	*/
/*			while (mSelectedServerList.size() <= mBudget) {
			randomServer = randomGenerator.getRandomNode(mServersNumber);
			//System.out.println(randomServer);
			if (canServerAccessSelectedServers(randomServer)) continue;
			
			//this is the number of caching cost
			mSelectedServerList.add(randomServer);
			
			for (User user : mUsers) {
				if (user.nearEdgeServers.contains(randomServer) && !mValidUserList.contains(user.id)) {
					mValidUserList.add(user.id);
				}
			}
	
			for (int i = 0; i < mBudget; i++) {
				int m = mServersNumber - 1;
				int j = m +(int)(Math.random()*(0+1-m));
				//System.out.println(j);
				if (mAdjacencyMatrix[j][randomServer] == 1 || mAdjacencyMatrix[randomServer][j] == 1) {
					// mConnectionsMap.remove(i);
					for (User user : mUsers) {
						if (user.nearEdgeServers.contains(j) && !mValidUserList.contains(user.id)) {
							mValidUserList.add(user.id);
						}
					}
				}
			}
		}
*/
	
	//�������������ȫ���û�ȫ����benefit�ĺ� Ȼ�󷵻���ȫ����benefit
	private double calculateBenefits() {
		mBenefitList.clear();
		for (int i = 0; i < mUsers.size(); i++) {
			mBenefitList.add(0);
		}

		double benefits = 0;
		
		for (User user : mUsers) {
			for (int server : user.nearEdgeServers) {
				int benefit = 1;
				if (mSelectedServerList.contains(server)) benefit = mUserBenefits[server][user.id];
				if (mBenefitList.get(user.id) < benefit) mBenefitList.set(user.id, benefit);
			}
		}
		
		//ÿһ��benefit����һ���û���benefit��Ȼ��benefits��ȫ���û���benefit���
		for (int benefit : mBenefitList) benefits = benefits + benefit;
		
		return benefits;
	}
	
	//������Ӧ�ü���ÿ���û���benefit��Ȼ���benefit������������ѭ��ÿ�����ֵ�ƽ���������Ϊfairness����ķ�ĸ
	private double calculate_single_Benefits_square() {
		mBenefitList.clear();
		for (int i = 0; i < mUsers.size(); i++) {
			mBenefitList.add(0);
		}

		double benefits = 0;
		
		for (User user : mUsers) {
			for (int server : user.nearEdgeServers) {
				int benefit = 1;
				if (mSelectedServerList.contains(server)) benefit = mUserBenefits[server][user.id];
				if (mBenefitList.get(user.id) < benefit) mBenefitList.set(user.id, benefit);
			}
		}
		
		//�������benefits��ָÿһ���û���benefit��ƽ����,�����Ļ����Է���benefits
		for (int benefit : mBenefitList) benefits = benefits + (benefit * benefit);
		
		return benefits;
	}
	
	private int[] calculate_user_covered_results_onetwothree_hops() {		
		//20220510
		for (EdgeServer server : mServers) {
			if (mSelectedServerList.contains(server.id)) {
				
				for (int j = 0; j < mUsers.size(); j++) {
					//0 hop 
					if (mUsers.get(j).nearEdgeServers.contains(server.id)) { //if mUsers's neighbor edge server list contain a server, add distribution result +1, need to break 		
						//0 hop access +1
						user_distribution_result[0]  += 1;
						//need to break, otherwise, the value may increase because there might be some other server cover the user as well
						//break;
					} else if (isConnected(mUsers.get(j).nearEdgeServers, server.id, mAdjacencyMatrix)) { //the server cover the user i is connected with a server in the selected server list
						//1 hop access +1
						user_distribution_result[1]  += 1;
						//break;
					} else if (istwohopsConnected(mUsers.get(j).nearEdgeServers, server.id, mDistanceMatrix)) { //the server cover the user i's distance with a server in the selected server list is 2
						//user j is served via 0 hop
						//userBenefits[i][j] = 4;
						//2 hops access +1
						user_distribution_result[2]  += 1;
						//break;
					} else if (isthreehopsConnected(mUsers.get(j).nearEdgeServers, server.id, mDistanceMatrix)) { //the server cover the user i's distance with a server in the selected server list is 3
						//3 hops access +1
						user_distribution_result[3]  += 1;					
					}
					else {
						//user j is served via more than 3 hops
						//userBenefits[i][j] = 0;
						//user_distribution_result[0] += 1;
						//break;
					}	
					
				}	
				
				break;				
			} 
			//break;				
		}
		
//		user_distribution_result[0] = zerohop_served_users;
//		user_distribution_result[1] = onehop_served_users;
//		user_distribution_result[2] = twohops_served_users;
//		user_distribution_result[3] = threehops_served_users;
		
		return user_distribution_result;
	}
	
	//if the servers are connected 1 hop
		private static boolean isConnected(List<Integer> servers, int server, int[][] adjacencyMatrix) {
			for (int s : servers) {
				if (adjacencyMatrix[s][server] == 1)
					return true;
			}
			return false;
		}
		
		//2 hops
		private static boolean istwohopsConnected(List<Integer> servers, int server, int[][] distanceMatrix) {
			for (int s : servers) {
				if (distanceMatrix[s][server] == 2 || distanceMatrix[server][s] == 2)
					return true;
			}

			return false;
		}
		//3 hops
		private static boolean isthreehopsConnected(List<Integer> servers, int server, int[][] distanceMatrix) {
			for (int s : servers) {
				if (distanceMatrix[s][server] == 3 || distanceMatrix[server][s] == 3)
					return true;
			}
			return false;
		}
	
		public double getAllUsers() {
			//return mAllUsers;
			return user_distribution_result[3];
		}
		
		//202204
		//get user distribution numbers
		public double getAllzeroUsers() {
			//return zerohop_served_users;
			return user_distribution_result[0];
		}
		
		public double getAlloneUsers() {
			return user_distribution_result[1];
		}
		
		public double getAlltwoUsers() {
			return user_distribution_result[2];
		}
		
		public double getAllthreeUsers() {
			return user_distribution_result[2];
		}
		
		//return array (0 hop, 1 hop, 2 hops, 3 hops) 2022
		public int[] get_user_distribution() {
			return user_distribution_result;
		}
				
	
	public double getcost() {
		return mCost;
	}
	
	public double getfairness_degree() {
		return mfairnessdegree;
	}
	
	public double getfairness_efficiency() {
		return mfairness_efficiency;
	}
	
	public double getallbenefits() {
		return mAllBenefits = mAllBenefits/mValidUserList.size();
	}
	
	public double getbenefit_efficiency() {
		return mbenefitefficiency = mAllBenefits/mCost;
	}
}
